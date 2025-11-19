package language.machine;

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

    public void declare(Compiler.MethodCompiler compiler, Repository repository, Scope scope, String name, List<GenericArgument> generics) {
        Scope variable = scope.state(this, name);

        for (int i = 0; i < this.generics.size(); i++) {
            Generic generic = this.generics.get(i);
            String genericName = generics.get(i).name;
            Scope.Generic g = new Scope.Generic();
            g.name = generic.name;
            g.known = true;
            switch (generic.type) {
                case Structure -> {
                    Structure value = repository.structure(genericName);
                    g.type = Scope.Generic.Type.Structure;
                    g.structure = value;
                    variable.put(generic.name, g);
                }
                case Document -> {
                    language.core.Document doc = repository.document(genericName);
                    g.type = Scope.Generic.Type.Document;
                    g.document = doc;
                    variable.put(generic.name, g);
                }
            }
        }
        for (Data v : this.variables) {
            if (v.size > 0) {
                int location = compiler.data(v.size);
                variable.put(v.name, new Scope.Allocation(v.size, location));
            }
        }
    }

    public void proxy(Repository repository, Scope variable, int location) {
        throw new RuntimeException("Not supported");
    }

    public void construct(Compiler.MethodCompiler compiler, Repository repository, Scope scope, String name, List<GenericArgument> generics, String constructorName, List<String> arguments) {
        Scope variable = scope.state(this, name);

        for (int i = 0; i < this.generics.size(); i++) {
            Generic generic = this.generics.get(i);
            String genericName = generics.get(i).name;
            Scope.Generic g = new Scope.Generic();
            g.name = generic.name;
            g.known = true;
            switch (generic.type) {
                case Structure -> {
                    Structure value = repository.structure(genericName);
                    g.type = Scope.Generic.Type.Structure;
                    g.structure = value;
                    variable.put(generic.name, g);
                }
                case Document -> {
                    language.core.Document doc = repository.document(genericName);
                    g.type = Scope.Generic.Type.Document;
                    g.document = doc;
                    variable.put(generic.name, g);
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
            variable.put(address, new Scope.Allocation(4, addr));
        }
        for (Data v : this.variables) {
            int location = compiler.data(v.size);
            variable.put(v.name, new Scope.Allocation(v.size, location));
        }

        Scope operationScope = scope.startOperation(variable, constructorName);
        c.populateScope(scope, operationScope, arguments);

        for (Statement s : c.body) {
            s.compile(compiler, repository, variable, operationScope);
        }
    }

    public void operate(Compiler.MethodCompiler compiler, Repository repository, Scope scope, Scope variable, String operationName, List<String> arguments) {
        // put the name instead of the arguments
        Scope operationScope = scope.startOperation(variable, operationName);
        operationScope.put("methodName", operationName);

        for (String arg : arguments) {
            Scope v = scope.findVariable(arg).orElseThrow(() -> new RuntimeException("Every reference argument should be a variable"));
            operationScope.put(arg, v);
        }

        for (Statement s : invokeOperation.body) {
            if (argStatement != s) {
                s.compile(compiler, repository, variable, operationScope);
                continue;
            }
            // For each argument, invoke the argMethod
            int argIndex = 0;
            for (String arg : arguments) {
                Scope v = scope.findVariable(arg).orElseThrow(() -> new RuntimeException("Every reference argument should be a variable"));
                operationScope.put(argOperation.parameters.get(0).name, v);

                operationScope.put("index", argIndex++);
                operationScope.put("methodName", operationName);
                for (Statement as : argOperation.body) {
                    as.compile(compiler, repository, variable, operationScope);
                }
            }
        }
    }

    public int size(Repository repository) {
        int total = 0;
        for (Data v : variables) {
            total+=v.size;
        }
        return total;
    }

    public static class ArgsStatement implements Statement {

        public void compile(Compiler.MethodCompiler compiler, Repository repository, Scope variable, Scope scope) {}

    }

    public static class ArgsCopyStatement implements Statement {

        ArrayList<String> arguments = new ArrayList<>();

        public void compile(Compiler.MethodCompiler compiler, Repository repository, Scope variable, Scope scope) {
            language.core.Document d;
            String methodName;

            InputType docInType = InputType.valueOf(this.arguments.get(0).toUpperCase());
            String input = this.arguments.get(1);
            d = switch (docInType) {
                case LG -> {
                    Scope.Generic g = scope.findGeneric(input).orElseThrow();
                    yield g.document;
                }
                case AG -> {
                    String[] split = input.split("\\.");
                    Scope var = scope.findVariable(split[0]).orElseThrow();
                    Scope.Generic g = var.findGeneric(split[1]).orElseThrow();
                    yield g.document;
                }
                default -> throw new RuntimeException();
            };

            InputType methodInType = InputType.valueOf(this.arguments.get(2).toUpperCase());
            input = this.arguments.get(3);
            methodName = switch (methodInType) {
                case I -> input;
                case L -> scope.findName(input).orElseThrow();
                default -> throw new RuntimeException();
            };

            InputType inputType = InputType.valueOf(this.arguments.get(4).toUpperCase());
            int index = inputType.resolve(this.arguments.get(5), scope).value();

            Scope argVariable = scope.findVariable(this.arguments.get(7)).orElseThrow();

            inputType = InputType.valueOf(this.arguments.get(8).toUpperCase());
            int addr = inputType.resolve(this.arguments.get(9), scope).value();

            language.core.Document.Method method = d.method(scope, methodName).orElseThrow();
            String param = method.parameters.get(index);
            if (param.equals(argVariable.structure().name())) {
                new InstructionStatement("m", "COPY", "TII", "LDA", "frameDataAddr", "ADA", "a", "ADS", "a").compile(compiler, repository, variable, scope);
                new InstructionStatement("i","ADD", "TI", "LDA", "frameDataAddr", "LDA", "frameDataAddr", "ADS", "a").compile(compiler, repository, variable, scope);
            } else if (param.equals("core.Pointer")) {
                int addrAddr = compiler.data(4);
                new InstructionStatement("i", "ADD", "LI", "I", "0d" + addrAddr, "R", "task", "ADA", "a").compile(compiler, repository, variable, scope);
                new InstructionStatement("m", "COPY", "TII", "LDA", "frameDataAddr", "I", "0d" + addrAddr, "ADS", "a").compile(compiler, repository, variable, scope);
                new InstructionStatement("i","ADD", "TI", "LDA", "frameDataAddr", "LDA", "frameDataAddr", "I", "0d4").compile(compiler, repository, variable, scope);
            } else {
                throw new RuntimeException("Can't copy to argument");
            }
        }

    }

}
