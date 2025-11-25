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
    public int size(Repository repository) {
        int maxSize = 0;
        for (Structure struct : structures) {
            maxSize = Math.max(struct.size(repository), maxSize);
        }
        return maxSize;
    }

    @Override
    public void declare(Compiler.MethodCompiler compiler, Repository repository, Scope scope, Scope.Generic descriptor, String name) {
        Scope thisVariable = scope.state(descriptor, name);

        for (String imprt : this.imports) {
            repository.structure(imprt);
        }

        int maxSize = size(repository);
        int typeLocation = compiler.data(TYPE_SIZE);
        thisVariable.put("type", new Scope.Allocation(TYPE_SIZE, typeLocation));
        int location = compiler.data(maxSize);
        for (Structure struct : structures) {
            SharedLocationMethodCompiler mc = new SharedLocationMethodCompiler();
            Scope.Generic description = new Scope.Generic();
            description.type = Scope.Generic.Type.Structure;
            description.structure = struct;
            description.generics = descriptor.generics;
            Scope structScope = thisVariable.state(description, struct.name);
            mc.parent = compiler;
            mc.location = location;
            for (Field f : struct.fields) {
                language.core.Structure u = repository.structure(f.descriptor.name);
                u.declare(mc, repository, structScope, descriptor, f.name);
            }
        }
    }

    @Override
    public void construct(Compiler.MethodCompiler compiler, Repository repository, Scope scope, Scope.Generic descriptor, String name, String constructorName, List<String> argumentNames) {
        Scope thisVariable = scope.state(descriptor, name);
        int maxSize = size(repository);
        int typeLocation = compiler.data(TYPE_SIZE);
        Scope.Allocation typeAllocation = new Scope.Allocation(TYPE_SIZE, typeLocation);
        thisVariable.put("type", typeAllocation);
        int location = compiler.data(maxSize);

        for (String imprt : this.imports) {
            repository.structure(imprt);
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

        Scope operationScope = scope.startOperation(thisVariable, structure.name);
        // Map the args to name using parameters
        int i = 0;
        for (Parameter param : method.params) {
            String argName = argumentNames.get(i++);
            switch (param.type) {
                case Variable -> {
                    Scope v = scope.findVariable(argName).orElseThrow();
                    operationScope.put(param.name, v);
                }
            }
        }

        int index = structures.indexOf(structure);
        for (Structure struct : structures) {
            SharedLocationMethodCompiler mc = new SharedLocationMethodCompiler();
            mc.parent = compiler;
            mc.location = location;
            Scope.Generic description = new Scope.Generic();
            description.type = Scope.Generic.Type.Structure;
            description.structure = struct;
            description.generics = descriptor.generics;
            Scope structureScope = thisVariable.state(description, struct.name);
            for (Field f : struct.fields) {
                language.core.Structure u = repository.structure(f.descriptor.name);
                Scope.Generic fieldDescription = new Scope.Generic();
                fieldDescription.type = Scope.Generic.Type.Structure;
                fieldDescription.structure = u;
                u.declare(mc, repository, structureScope, fieldDescription, f.name);
            }
        }

        SharedLocationMethodCompiler mc = new SharedLocationMethodCompiler();
        mc.parent = compiler;
        mc.location = location;
        new InstructionStatement("i", "ADD", "II", "P", "type", "I", "0d0", "I", "0d" + (index + 1))
                .compile(compiler, repository, thisVariable, operationScope);
        Scope structureScope = thisVariable.state(descriptor, structure.name);
        Scope structConstructorScope = operationScope.startOperation(structureScope, "?");
        for (Statement stmt : method.statements) {
            stmt.handle(mc, repository, structConstructorScope);
        }
    }

    @Override
    public void operate(Compiler.MethodCompiler compiler, Repository repository, Scope scope, Scope variable, String operationName, List<String> arguments) {
        Scope operationScope = scope.startOperation(variable, operationName);
        switch (operationName) {
            case "match" -> {
                if (arguments.size() % 2 != 0) { throw new RuntimeException("Should be type followed by block for every type"); }
                ArrayList<String> types = new ArrayList<>();
                // add all names to types
                int[] blockPositions = new int[arguments.size() / 2];

                new InstructionStatement("j", "REL", "T", "P", "type").compile(compiler, repository, variable, operationScope);
                int jumps = compiler.address();
                operationScope.putAddress("jumps", jumps);
                int cases = compiler.address();
                operationScope.putAddress("cases", cases);
                int end = compiler.address();
                operationScope.putAddress("end", end);
                for (int i = 0; i < arguments.size(); i+=2) {
                    int caseAddr = compiler.address();
                    operationScope.putAddress("case", caseAddr);
                    int prev = compiler.position(jumps);
                    new InstructionStatement("j", "REL", "I", "case").compile(compiler, repository, variable, operationScope);
                    compiler.position(cases);
                    compiler.address(caseAddr);

                    // execute block
                    Structure structure = structures.get(i / 2);
                    Scope structScope = variable.findVariable(structure.name).orElseThrow();
                    operationScope.implicit().put(structure.name, structScope);
                    String arg = arguments.get(i + 1);
                    Scope.Block b = scope.findBlock(arg).orElse(null);
                    if (b == null) {
                        throw new RuntimeException("Expecting block for union type");
                    }
                    b.execute(compiler, operationScope);
                    operationScope.implicit().removeVariable(structure.name);

                    new InstructionStatement("j", "REL", "I", "end").compile(compiler, repository, variable, operationScope);
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

                new InstructionStatement("j", "REL", "T", "P", "type").compile(compiler, repository, variable, operationScope);
                int jumps = compiler.address();
                operationScope.putAddress("jumps", jumps);
                int cases = compiler.address();
                operationScope.putAddress("cases", cases);
                int end = compiler.address();
                operationScope.putAddress("end", end);
                int i = 0;
                for (Structure s : structures) {
                    int caseAddr = compiler.address();
                    operationScope.putAddress("case", caseAddr);
                    int prev = compiler.position(jumps);
                    new InstructionStatement("j", "REL", "I", "case").compile(compiler, repository, variable, operationScope);
                    compiler.position(cases);
                    compiler.address(caseAddr);

                    // execute block
                    Scope structScope = variable.findVariable(s.name).orElseThrow();
                    Scope methodScope = scope.startOperation(structScope, operationName);
                    Method m = methods.get(i);
                    for (Statement stmt : m.statements) {
                        int argI = 0;
                        for (Parameter p : m.params) {
                            String arg = arguments.get(argI++);
                            switch (p.type) {
                                case Variable -> {
                                    Scope v = scope.findVariable(arg).orElseThrow();
                                    methodScope.put(p.name, v);
                                }
                            }
                        }
                        stmt.handle(compiler, repository, methodScope);
                    }

                    new InstructionStatement("j", "REL", "I", "end").compile(compiler, repository, variable, operationScope);
                    compiler.address(end);
                    compiler.position(prev);
                    i++;
                }
            }
        }
    }
}
