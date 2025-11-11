package language.enumeration;

import language.core.*;
import language.machine.InstructionStatement;

import java.util.*;

public class EnumStructure implements language.core.Structure {

    static final int TYPE_SIZE = 1;

    String name;
    ArrayList<String> imports = new ArrayList<>();
    ArrayList<String> dependencies = new ArrayList<>();
    ArrayList<Structure> structures = new ArrayList<>();

    @Override
    public String name() {
        return name;
    }

    @Override
    public int size(Sources sources) {
        int maxSize = 0;
        for (Structure struct : structures) {
            maxSize = Math.max(struct.size(sources), maxSize);
        }
        return maxSize;
    }

    @Override
    public void proxy(Sources sources, Scope variable, int location) {

    }

    @Override
    public void declare(Compiler.MethodCompiler compiler, Sources sources, Scope scope, String name, List<String> generics) {
        Scope thisVariable = scope.state(name);
        thisVariable.name = name;
        thisVariable.structure = this;

        for (String imprt : this.imports) {
            sources.structure(imprt);
        }

        int maxSize = size(sources);
        int typeLocation = compiler.data(TYPE_SIZE);
        thisVariable.allocations.put("type", new Scope.Allocation(TYPE_SIZE, typeLocation));
        compiler.debugData(thisVariable.stateName(thisVariable.name), "type", typeLocation, TYPE_SIZE);
        int location = compiler.data(maxSize);
        for (Structure struct : structures) {
            SharedLocationMethodCompiler mc = new SharedLocationMethodCompiler();
            Scope structScope = thisVariable.state(struct.name);
            mc.parent = compiler;
            mc.location = location;
            for (Field f : struct.fields) {
                language.core.Structure u = sources.structure(f.structure);
                u.declare(mc, sources, structScope, f.name, f.generics);
            }
        }
    }

    @Override
    public void construct(Compiler.MethodCompiler compiler, Sources sources, Scope scope, String name, List<String> generics, String constructorName, List<Argument> arguments) {
        Scope thisVariable = scope.state(name);
        thisVariable.name = name;
        thisVariable.structure = this;

        for (String imprt : this.imports) {
            sources.structure(imprt);
        }

        Structure structure = null;
        Method method = null;
        root: for (Structure s : structures) {
            for (Method m : s.constructors) {
                if (m.name.equals(constructorName)) {
                    method = m;
                    structure = s;
                    break root;
                }
            }
        }
        if (method == null) { throw new RuntimeException("Method not found"); }

        // Map the args to name using parameters
        HashMap<String, language.core.Argument> argsByName = new HashMap<>();
        int i = 0;
        for (Parameter param : method.params) {
            argsByName.put(param.name, arguments.get(i++));
        }

        String constructorStructName = "";
        int maxSize = size(sources);
        int typeLocation = compiler.data(TYPE_SIZE);
        Scope.Allocation typeAllocation = new Scope.Allocation(TYPE_SIZE, typeLocation);
        thisVariable.allocations.put("type", typeAllocation);
        compiler.debugData(thisVariable.stateName(thisVariable.name), "type", typeLocation, TYPE_SIZE);
        int location = compiler.data(maxSize);
        int index = structures.indexOf(structure);
        for (Structure struct : structures) {
            if (struct == structure) {
                constructorStructName = struct.name;
            }
            SharedLocationMethodCompiler mc = new SharedLocationMethodCompiler();
            mc.parent = compiler;
            mc.location = location;
            Scope structureScope = thisVariable.state(struct.name);
            for (Field f : struct.fields) {
                language.core.Structure u = sources.structure(f.structure);
                u.declare(mc, sources, structureScope, f.name, f.generics);
            }
        }

        Scope operationScope = thisVariable.startOperation(constructorStructName);
        SharedLocationMethodCompiler mc = new SharedLocationMethodCompiler();
        mc.parent = compiler;
        mc.location = location;
        new InstructionStatement("i", "ADD", "II", "LDA", "type", "IL", "0d0", "IL", "0d" + (index + 1))
                .compile(compiler, sources, thisVariable, Map.of(), operationScope);
        Scope structureScope = thisVariable.state(constructorStructName);
        for (Statement stmt : method.statements) {
            stmt.handle(mc, sources, argsByName, structureScope);
        }
    }

    @Override
    public void operate(Compiler.MethodCompiler compiler, Sources sources, Scope scope, Scope variable, String operationName, List<Argument> arguments) {
        Scope operationScope = variable.startOperation(operationName);
        switch (operationName) {
            case "match" -> {
                if (arguments.size() % 2 != 0) { throw new RuntimeException("Should be type followed by block for every type"); }
                for (int i = 0; i < arguments.size(); i+=2) {
                    if (arguments.get(i).type != Argument.Type.Name) { throw new RuntimeException("Expecting union type"); }
                    if (arguments.get(i + 1).type != Argument.Type.Block) { throw new RuntimeException("Expecting block for union type"); }
                }
                ArrayList<String> types = new ArrayList<>();
                // add all names to types
                int[] blockPositions = new int[arguments.size() / 2];

                new InstructionStatement("j", "REL", "T", "LDA", "type").compile(compiler, sources, variable, Map.of(), operationScope);
                int jumps = compiler.address();
                Scope.Allocation allocation = new Scope.Allocation(4, jumps);
                operationScope.add("jumps", allocation);
                int cases = compiler.address();
                allocation = new Scope.Allocation(4, cases);
                operationScope.add("cases", allocation);
                int end = compiler.address();
                allocation = new Scope.Allocation(4, end);
                operationScope.add("end", allocation);
                for (int i = 0; i < arguments.size(); i+=2) {
                    int caseAddr = compiler.address();
                    allocation = new Scope.Allocation(4, caseAddr);
                    operationScope.add("case", allocation);
                    int prev = compiler.position(jumps);
                    new InstructionStatement("j", "REL", "I", "case").compile(compiler, sources, variable, Map.of(), operationScope);
                    compiler.position(cases);
                    compiler.address(caseAddr);

                    // execute block
                    Structure structure = structures.get(i / 2);
                    Scope structScope = variable.state(structure.name);
                    structScope.structure = structure;
                    structScope.implicit.put(structure.name, structScope);
                    arguments.get(i + 1).block.execute(compiler, structScope);
                    structScope.implicit.remove(structure.name);

                    new InstructionStatement("j", "REL", "I", "end").compile(compiler, sources, variable, Map.of(), operationScope);
                    compiler.address(end);
                    compiler.position(prev);
                }
            }
            default -> {
                ArrayList<Method> methods = new ArrayList<>();
                boolean allImplement = true;
                for (Structure s : structures) {
                    boolean found = false;
                    for (Method m : s.methods) {
                        if (m.name.equals(operationName)) {
                            methods.add(m);
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        allImplement = false;
                        break;
                    }
                }
                if (!allImplement) {
                    throw new RuntimeException("Unexpected union method");
                }

                new InstructionStatement("j", "REL", "T", "LDA", "type").compile(compiler, sources, variable, Map.of(), operationScope);
                int jumps = compiler.address();
                Scope.Allocation allocation = new Scope.Allocation(4, jumps);
                operationScope.add("jumps", allocation);
                int cases = compiler.address();
                allocation = new Scope.Allocation(4, cases);
                operationScope.add("cases", allocation);
                int end = compiler.address();
                allocation = new Scope.Allocation(4, end);
                operationScope.add("end", allocation);
                int i = 0;
                for (Structure s : structures) {
                    int caseAddr = compiler.address();
                    allocation = new Scope.Allocation(4, caseAddr);
                    operationScope.add("case", allocation);
                    int prev = compiler.position(jumps);
                    new InstructionStatement("j", "REL", "I", "case").compile(compiler, sources, variable, Map.of(), operationScope);
                    compiler.position(cases);
                    compiler.address(caseAddr);

                    // execute block
                    Scope structScope = variable.state(s.name);
                    structScope.structure = s;
                    Scope methodScope = structScope.startOperation(operationName);
                    Method m = methods.get(i);
                    for (Statement stmt : m.statements) {
                        HashMap<String, Argument> args = new HashMap<>();
                        int argI = 0;
                        for (Parameter p : m.params) {
                            args.put(p.name, arguments.get(argI++));
                        }
                        stmt.handle(compiler, sources, args, methodScope);
                    }

                    new InstructionStatement("j", "REL", "I", "end").compile(compiler, sources, variable, Map.of(), operationScope);
                    compiler.address(end);
                    compiler.position(prev);
                    i++;
                }
            }
        }
    }
}
