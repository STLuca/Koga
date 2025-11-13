package language.machine;

import core.Document;
import language.core.*;
import language.core.Compiler;

import java.util.*;

public class MachineReferenceStructure implements Structure {

    String name;
    ArrayList<Data> variables = new ArrayList<>();
    ArrayList<String> addresses = new ArrayList<>();
    ArrayList<Operation> constructors = new ArrayList<>();
    ArrayList<Generic> generics = new ArrayList<>();
    Operation invokeOperation;
    Statement argStatement;
    Operation argOperation;
    
    public String name() {
        return name;
    }

    public void declare(Compiler.MethodCompiler compiler, Sources sources, Scope scope, String name, List<String> generics, List<GenericArgument> nestedGenerics) {
        Scope variable = scope.add(name);
        variable.name = name;
        variable.structure = this;

        for (int i = 0; i < this.generics.size(); i++) {
            Generic generic = this.generics.get(i);
            Scope.Generic g = new Scope.Generic();
            g.name = generic.name;
            g.known = true;
            switch (generic.type) {
                case Structure -> {
                    Structure value = sources.structure(generics.get(i));
                    g.type = Scope.Generic.Type.Structure;
                    g.structure = value;
                    variable.generics.put(generic.name, g);
                }
                case Document -> {
                    Document doc = sources.document(generics.get(i), Compilable.Level.Head);
                    g.type = Scope.Generic.Type.Document;
                    g.document = doc;
                    variable.generics.put(generic.name, g);
                }
            }
        }
        for (Data v : this.variables) {
            if (v.size > 0) {
                int location = compiler.data(v.size);
                variable.allocations.put(v.name, new Scope.Allocation(v.size, location));
                compiler.debugData(variable.stateName(v.name), v.name, location, v.size);
            }
        }
    }

    public void proxy(Sources sources, Scope variable, int location) {
        throw new RuntimeException("Not supported");
    }

    public void construct(Compiler.MethodCompiler compiler, Sources sources, Scope scope, String name, List<String> generics, List<GenericArgument> nestedGenerics, String constructorName, List<String> arguments) {
        Scope variable = scope.add(name);
        variable.name = name;
        variable.structure = this;

        for (int i = 0; i < this.generics.size(); i++) {
            Generic generic = this.generics.get(i);
            Scope.Generic g = new Scope.Generic();
            g.name = generic.name;
            g.known = true;
            switch (generic.type) {
                case Structure -> {
                    Structure value = sources.structure(generics.get(i));
                    g.type = Scope.Generic.Type.Structure;
                    g.structure = value;
                    variable.generics.put(generic.name, g);
                }
                case Document -> {
                    Document doc = sources.document(generics.get(i), Compilable.Level.Head);
                    g.type = Scope.Generic.Type.Document;
                    g.document = doc;
                    variable.generics.put(generic.name, g);
                }
            }
        }

        // Try and match a constructor
        Operation c = null;
        for (Operation con : constructors) {
            if (con.matches(variable, scope, constructorName, arguments)) {
                c = con;
                break;
            }
        }
        if (c == null) {
            throw new RuntimeException("No constructor found");
        }

        for (String address : addresses) {
            int addr = compiler.address();
            variable.allocations.put(address, new Scope.Allocation(4, addr));
        }
        for (Data v : this.variables) {
            int location = compiler.data(v.size);
            variable.allocations.put(v.name, new Scope.Allocation(v.size, location));
            compiler.debugData(variable.stateName(v.name), v.name, location, v.size);
        }

        Scope operationScope = scope.startOperation(constructorName);
        int argIdx = 0;
        for (Operation.Parameter p : c.parameters) {
            String arg = arguments.get(argIdx++);
            switch(p.type) {
                case Literal -> {
                    int literal = scope.findLiteral(arg).orElseThrow();
                    operationScope.literals.put(p.name, literal);
                }
                case Variable -> {
                    Scope v = scope.findVariable(arg);
                    if (v == null) { throw new RuntimeException(); }
                    operationScope.scopes.put(p.name, v);
                }
                case Block -> {
                    Block b = scope.findBlock(arg).orElseThrow();
                    operationScope.blocks.put(p.name, b);
                }
                case Name -> {
                    operationScope.names.put(p.name, arg);
                }
            }
        }
        for (Statement s : c.body) {
            s.compile(compiler, sources, variable, operationScope);
        }
    }

    public void operate(Compiler.MethodCompiler compiler, Sources sources, Scope scope, Scope variable, String operationName, List<String> arguments) {
        // put the name instead of the arguments
        Scope operationScope = scope.startOperation(operationName);
        operationScope.names.put("methodName", operationName);

        for (String arg : arguments) {
            Scope v = scope.findVariable(arg);
            if (v == null) { throw new RuntimeException("Every reference argument should be a variable"); }
            operationScope.scopes.put(arg, v);
        }

        for (Statement s : invokeOperation.body) {
            if (argStatement != s) {
                s.compile(compiler, sources, variable, operationScope);
                continue;
            }
            // For each argument, invoke the argMethod
            int argIndex = 0;
            for (String arg : arguments) {
                Scope v = scope.findVariable(arg);
                if (v == null) { throw new RuntimeException("Every reference argument should be a variable"); }
                operationScope.scopes.put(argOperation.parameters.get(0).name, v);

                operationScope.literals.put("index", argIndex++);
                operationScope.names.put("methodName", operationName);
                for (Statement as : argOperation.body) {
                    as.compile(compiler, sources, variable, operationScope);
                }
            }
        }
    }

    public int size(Sources sources) {
        int total = 0;
        for (Data v : variables) {
            total+=v.size;
        }
        return total;
    }

    public static class ArgsStatement implements Statement {

        public void compile(Compiler.MethodCompiler compiler, Sources sources, Scope variable, Scope scope) {}

    }

    public static class ArgsCopyStatement implements Statement {

        ArrayList<String> arguments = new ArrayList<>();

        public void compile(Compiler.MethodCompiler compiler, Sources sources, Scope variable, Scope scope) {
            Document d;
            String methodName;

            InputType docInType = InputType.valueOf(this.arguments.get(0).toUpperCase());
            String input = this.arguments.get(1);
            d = switch (docInType) {
                case LG -> {
                    Scope.Generic g = variable.generics.get(input);
                    yield g.document;
                }
                case AG -> {
                    String[] split = input.split("\\.");
                    Scope var = scope.findVariable(split[0]);
                    Scope.Generic g = var.generics.get(split[1]);
                    yield g.document;
                }
                default -> throw new RuntimeException();
            };

            InputType methodInType = InputType.valueOf(this.arguments.get(2).toUpperCase());
            input = this.arguments.get(3);
            methodName = switch (methodInType) {
                case IL -> input;
                case AL -> scope.findName(input).orElseThrow();
                default -> throw new RuntimeException();
            };

            InputType inputType = InputType.valueOf(this.arguments.get(4).toUpperCase());
            int index = inputType.resolve(this.arguments.get(5), variable, scope).value();

            Scope argVariable = scope.findVariable(this.arguments.get(7));

            inputType = InputType.valueOf(this.arguments.get(8).toUpperCase());
            int addr = inputType.resolve(this.arguments.get(9), variable, scope).value();

            core.Document.Method method = null;
            for (core.Document.Method m : d.methods) {
                if (m.name.equals(methodName)) {
                    method = m;
                    break;
                }
            }
            String param = method.parameters[index];
            if (param.equals(argVariable.structure.name())) {
                new InstructionStatement("m", "COPY", "TII", "LDA", "frameDataAddr", "ADA", "a", "ADS", "a").compile(compiler, sources, variable, scope);
                new InstructionStatement("i","ADD", "TI", "LDA", "frameDataAddr", "LDA", "frameDataAddr", "ADS", "a").compile(compiler, sources, variable, scope);
            } else if (param.equals("core.Pointer")) {
                int addrAddr = compiler.data(4);
                new InstructionStatement("i", "ADD", "LI", "IL", "0d" + addrAddr, "R", "task", "ADA", "a").compile(compiler, sources, variable, scope);
                new InstructionStatement("m", "COPY", "TII", "LDA", "frameDataAddr", "IL", "0d" + addrAddr, "ADS", "a").compile(compiler, sources, variable, scope);
                new InstructionStatement("i","ADD", "TI", "LDA", "frameDataAddr", "LDA", "frameDataAddr", "IL", "0d4").compile(compiler, sources, variable, scope);
            } else {
                throw new RuntimeException("Can't copy to argument");
            }
        }

    }

}
