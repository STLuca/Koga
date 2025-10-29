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

    @Override
    public int genericIndex(String name) {
        int i = 0;
        for (Generic g : generics) {
            if (g.name.equals(name)) {
                return i;
            }
            i++;
        }
        return -1;
    }

    public void declare(Compiler.MethodCompiler compiler, Sources sources, Map<String, Variable> variables, String name, List<String> generics) {
        Variable variable = new Variable();
        variable.name = name;
        variable.structure = this;
        variables.put(name, variable);
        for (int i = 0; i < this.generics.size(); i++) {
            Generic generic = this.generics.get(i);
            switch (generic.type) {
                case Structure -> {
                    Structure value = sources.structure(generics.get(i));
                    Variable.Generic g = new Variable.Generic();
                    g.type = Variable.Generic.Type.Structure;
                    g.structure = value;
                    variable.generics.add(g);
                }
                case Document -> {
                    Document doc = sources.document(generics.get(i), Compilable.Level.Head);
                    Variable.Generic g = new Variable.Generic();
                    g.type = Variable.Generic.Type.Document;
                    g.document = doc;
                    variable.generics.add(g);
                }
            }
        }
    }

    public void proxy(Sources sources, Variable variable, int location) {
        throw new RuntimeException("Not supported");
    }

    public void construct(Compiler.MethodCompiler compiler, Sources sources, Map<String, Variable> variables, String name, List<String> generics, String constructorName, List<Argument> args, Context context) {
        Variable variable = new Variable();
        variable.name = name;
        variable.structure = this;
        variables.put(name, variable);

        for (int i = 0; i < this.generics.size(); i++) {
            Generic generic = this.generics.get(i);
            switch (generic.type) {
                case Structure -> {
                    Structure value = sources.structure(generics.get(i));
                    Variable.Generic g = new Variable.Generic();
                    g.type = Variable.Generic.Type.Structure;
                    g.structure = value;
                    variable.generics.add(g);
                }
                case Document -> {
                    Document doc = sources.document(generics.get(i), Compilable.Level.Head);
                    Variable.Generic g = new Variable.Generic();
                    g.type = Variable.Generic.Type.Document;
                    g.document = doc;
                    variable.generics.add(g);
                }
            }
        }

        // Try and match a constructor
        Operation c = null;
        for (Operation con : constructors) {
            if (con.matches(variable, constructorName, args)) {
                c = con;
                break;
            }
        }
        if (c == null) {
            throw new RuntimeException("No constructor found");
        }

        // Map the args to their name using referencedClass and parameters
        HashMap<String, Argument> argsByName = new HashMap<>();

        // setup parameter arguments
        int i = 0;
        for (Operation.Parameter p : c.parameters) {
            argsByName.put(p.name, args.get(i));
            i++;
        }

        for (String address : addresses) {
            int addr = compiler.address();
            variable.allocations.put(address, new Variable.Allocation(4, addr));
        }
        for (Data v : this.variables) {
            int location = compiler.data(v.size);
            variable.allocations.put(v.name, new Variable.Allocation(v.size, location));
            compiler.debugData(variable.name, v.name, location, v.size);
        }

        variable.methodAllocations.push(new HashMap<>());
        for (Statement s : c.body) {
            s.compile(compiler, sources, variable, argsByName, context);
        }
        variable.methodAllocations.pop();
    }

    public void operate(Compiler.MethodCompiler compiler, Sources sources, Map<String, Variable> variables, Variable variable, String operationName, List<Argument> args, Context context) {
        HashMap<String, Argument> argsByName = new HashMap<>();

        // put the name instead of the arguments
        Argument methodNameArg = Argument.of(operationName);
        argsByName.put("methodName", methodNameArg);

        variable.methodAllocations.push(new HashMap<>());

        for (Statement s : invokeOperation.body) {
            if (argStatement != s) {
                s.compile(compiler, sources, variable, argsByName, context);
                continue;
            }
            // For each argument, invoke the argMethod
            int argIndex = 0;
            HashMap<String, Argument> argArgs = new HashMap<>();
            for (Argument arg : args) {
                argArgs.put(argOperation.parameters.get(0).name, arg);
                argArgs.put("index", Argument.of(argIndex++));
                argArgs.put("methodName", methodNameArg);
                for (Statement as : argOperation.body) {
                    as.compile(compiler, sources, variable, argArgs, context);
                }
            }
        }
        variable.methodAllocations.pop();
    }

    public int size(Sources sources) {
        int total = 0;
        for (Data v : variables) {
            total+=v.size;
        }
        return total;
    }

    public static class ArgsStatement implements Statement {

        public void compile(Compiler.MethodCompiler compiler, Sources sources, Variable variable, Map<String, Argument> arguments, Context context) {}

    }

    public static class ArgsCopyStatement implements Statement {

        ArrayList<String> arguments = new ArrayList<>();

        public void compile(Compiler.MethodCompiler compiler, Sources sources, Variable variable, Map<String, Argument> arguments, Context context) {
            Document d;
            String methodName;

            InputType docInType = InputType.valueOf(this.arguments.get(0).toUpperCase());
            String input = this.arguments.get(1);
            d = switch (docInType) {
                case LG -> {
                    int index = variable.structure.genericIndex(input);
                    Variable.Generic g = variable.generics.get(index);
                    yield g.document;
                }
                case AG -> {
                    String[] split = input.split("\\.");
                    Variable var = arguments.get(split[0]).variable;
                    int index = var.structure.genericIndex(split[1]);
                    Variable.Generic g = var.generics.get(index);
                    yield g.document;
                }
                default -> throw new RuntimeException();
            };

            InputType methodInType = InputType.valueOf(this.arguments.get(2).toUpperCase());
            input = this.arguments.get(3);
            methodName = switch (methodInType) {
                case IL -> input;
                case AL -> arguments.get(input).name;
                default -> throw new RuntimeException();
            };

            InputType inputType = InputType.valueOf(this.arguments.get(4).toUpperCase());
            int index = inputType.resolve(this.arguments.get(5), variable, arguments, context).value();

            Variable argVariable = arguments.get(this.arguments.get(7)).variable;

            inputType = InputType.valueOf(this.arguments.get(8).toUpperCase());
            int addr = inputType.resolve(this.arguments.get(9), variable, arguments, context).value();

            core.Document.Method method = null;
            for (core.Document.Method m : d.methods) {
                if (m.name.equals(methodName)) {
                    method = m;
                    break;
                }
            }
            String param = method.parameters[index];
            if (param.equals(argVariable.structure.name())) {
                new InstructionStatement("m", "COPY", "TII", "LDA", "frameDataAddr", "ADA", "a", "ADS", "a").compile(compiler, sources, variable, arguments, context);
                new InstructionStatement("i","ADD", "TI", "LDA", "frameDataAddr", "LDA", "frameDataAddr", "ADS", "a").compile(compiler, sources, variable, arguments, context);
            } else if (param.equals("core.Pointer")) {
                int addrAddr = compiler.data(4);
                new InstructionStatement("i", "ADD", "LI", "IL", "0d" + addrAddr, "R", "task", "ADA", "a").compile(compiler, sources, variable, arguments, context);
                new InstructionStatement("m", "COPY", "TII", "LDA", "frameDataAddr", "IL", "0d" + addrAddr, "ADS", "a").compile(compiler, sources, variable, arguments, context);
                new InstructionStatement("i","ADD", "TI", "LDA", "frameDataAddr", "LDA", "frameDataAddr", "IL", "0d4").compile(compiler, sources, variable, arguments, context);
            } else {
                throw new RuntimeException("Can't copy to argument");
            }
        }

    }

}
