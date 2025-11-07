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
    public void proxy(Sources sources, Variable variable, int location) {

    }

    @Override
    public void declare(Compiler.MethodCompiler compiler, Sources sources, Context context, String name, List<String> generics) {
        Variable thisVariable = new Variable();
        thisVariable.name = name;
        thisVariable.structure = this;
        context.add(thisVariable);

        for (String imprt : this.imports) {
            sources.parse(imprt);
        }

        int maxSize = size(sources);
        int typeLocation = compiler.data(TYPE_SIZE);
        thisVariable.allocations.put("type", new Variable.Allocation(TYPE_SIZE, typeLocation));
        compiler.debugData(thisVariable.name, "type", typeLocation, TYPE_SIZE);
        int location = compiler.data(maxSize);
        for (Structure struct : structures) {
            SharedLocationMethodCompiler mc = new SharedLocationMethodCompiler();
            mc.parent = compiler;
            mc.location = location;
            for (Field f : struct.fields) {
                language.core.Structure u = sources.structure(f.structure);
                String fieldName = name + "." + struct.name + "." + f.name;
                u.declare(mc, sources, context, fieldName, f.generics);
            }
        }
    }

    @Override
    public void construct(Compiler.MethodCompiler compiler, Sources sources, Context context, String name, List<String> generics, String constructorName, List<Argument> arguments) {
        Variable thisVariable = new Variable();
        thisVariable.name = name;
        thisVariable.structure = this;
        context.add(thisVariable);

        for (String imprt : this.imports) {
            sources.parse(imprt);
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

        context.startOperation();

        String constructorStructName = "";
        int maxSize = size(sources);
        int typeLocation = compiler.data(TYPE_SIZE);
        Variable.Allocation typeAllocation = new Variable.Allocation(TYPE_SIZE, typeLocation);
        thisVariable.allocations.put("type", typeAllocation);
        compiler.debugData(thisVariable.name, "type", typeLocation, TYPE_SIZE);
        int location = compiler.data(maxSize);

        int index = structures.indexOf(structure);
        new InstructionStatement("i", "ADD", "II", "LDA", "type", "IL", "0d0", "IL", "0d" + (index + 1))
                .compile(compiler, sources, thisVariable, Map.of(), context);
        for (Structure struct : structures) {
             if (struct == structure) {
                 constructorStructName = struct.name;
             }
            SharedLocationMethodCompiler mc = new SharedLocationMethodCompiler();
            mc.parent = compiler;
            mc.location = location;
            for (Field f : struct.fields) {
                language.core.Structure u = sources.structure(f.structure);
                String fieldName = name + "." + struct.name + "." + f.name;
                u.declare(mc, sources, context, fieldName, f.generics);
            }
        }

        SharedLocationMethodCompiler mc = new SharedLocationMethodCompiler();
        mc.parent = compiler;
        mc.location = location;
        for (Statement stmt : method.statements) {
            stmt.handle(mc, sources, argsByName, name + "." + constructorStructName, context);
        }
        context.stopOperation();
    }

    @Override
    public void operate(Compiler.MethodCompiler compiler, Sources sources, Context context, Variable variable, String operationName, List<Argument> arguments) {
        context.startOperation();
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

                new InstructionStatement("j", "REL", "T", "LDA", "type").compile(compiler, sources, variable, Map.of(), context);
                int jumps = compiler.address();
                Variable.Allocation allocation = new Variable.Allocation(4, jumps);
                context.operationAllocation("jumps", allocation);
                int cases = compiler.address();
                allocation = new Variable.Allocation(4, cases);
                context.operationAllocation("cases", allocation);
                int end = compiler.address();
                allocation = new Variable.Allocation(4, end);
                context.operationAllocation("end", allocation);
                for (int i = 0; i < arguments.size(); i+=2) {
                    int caseAddr = compiler.address();
                    allocation = new Variable.Allocation(4, caseAddr);
                    context.operationAllocation("case", allocation);
                    int prev = compiler.position(jumps);
                    new InstructionStatement("j", "REL", "I", "case").compile(compiler, sources, variable, Map.of(), context);
                    compiler.position(cases);
                    compiler.address(caseAddr);

                    // execute block
                    variable.structure = structures.get(i / 2);
                    arguments.get(i + 1).block.execute(compiler);
                    variable.structure = this;


                    new InstructionStatement("j", "REL", "I", "end").compile(compiler, sources, variable, Map.of(), context);
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

                new InstructionStatement("j", "REL", "T", "LDA", "type").compile(compiler, sources, variable, Map.of(), context);
                int jumps = compiler.address();
                Variable.Allocation allocation = new Variable.Allocation(4, jumps);
                context.operationAllocation("jumps", allocation);
                int cases = compiler.address();
                allocation = new Variable.Allocation(4, cases);
                context.operationAllocation("cases", allocation);
                int end = compiler.address();
                allocation = new Variable.Allocation(4, end);
                context.operationAllocation("end", allocation);
                int i = 0;
                for (Structure s : structures) {
                    int caseAddr = compiler.address();
                    allocation = new Variable.Allocation(4, caseAddr);
                    context.operationAllocation("case", allocation);
                    int prev = compiler.position(jumps);
                    new InstructionStatement("j", "REL", "I", "case").compile(compiler, sources, variable, Map.of(), context);
                    compiler.position(cases);
                    compiler.address(caseAddr);

                    // execute block
                    variable.structure = structures.get(i / 2);
                    Method m = methods.get(i);
                    for (Statement stmt : m.statements) {
                        HashMap<String, Argument> args = new HashMap<>();
                        int argI = 0;
                        for (Parameter p : m.params) {
                            args.put(p.name, arguments.get(argI++));
                        }
                        stmt.handle(compiler, sources, args, variable.name + "." + s.name, context);
                    }
                    variable.structure = this;

                    new InstructionStatement("j", "REL", "I", "end").compile(compiler, sources, variable, Map.of(), context);
                    compiler.address(end);
                    compiler.position(prev);
                    i++;
                }
            }
        }
        context.stopOperation();
    }
}
