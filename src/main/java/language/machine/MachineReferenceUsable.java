package language.machine;

import core.Document;
import language.core.*;
import language.core.Compiler;

import java.util.*;

public class MachineReferenceUsable implements Usable {

    String name;
    ArrayList<Data> variables = new ArrayList<>();
    ArrayList<String> addresses = new ArrayList<>();
    ArrayList<Method> constructors = new ArrayList<>();
    ArrayList<Generic> generics = new ArrayList<>();
    Method invokeMethod;
    Statement argStatement;
    Method argMethod;
    
    public String name() {
        return name;
    }
    
    public void declare(Compiler.MethodCompiler compiler, Sources sources, Map<String, Variable> variables, String name, List<String> generics) {
        Variable variable = new Variable();
        variable.name = name;
        variable.usable = this;
        variables.put(name, variable);
        for (int i = 0; i < this.generics.size(); i++) {
            Generic generic = this.generics.get(i);
            switch (generic.type) {
                case Usable -> {
                    Usable value = sources.usable(generics.get(i));
                    variable.generics.put(generic.name, value);
                }
                case Document -> {
                    Document doc = sources.document(generics.get(i));
                    variable.documents.put(generic.name, doc);
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
        variable.usable = this;
        variables.put(name, variable);

        for (int i = 0; i < this.generics.size(); i++) {
            Generic generic = this.generics.get(i);
            switch (generic.type) {
                case Usable -> {
                    Usable value = sources.usable(generics.get(i));
                    variable.generics.put(generic.name, value);
                }
                case Document -> {
                    Document doc = sources.document(generics.get(i));
                    variable.documents.put(generic.name, doc);
                }
            }
        }

        // Try and match a constructor
        Method c = null;
        for (Method con : constructors) {
            if (con.matches(variable, constructorName, args)) {
                c = con;
                break;
            }
        }
        if (c == null) throw new RuntimeException("No constructor found");

        // Map the args to their name using referencedClass and parameters
        HashMap<String, Argument> argsByName = new HashMap<>();

        // setup parameter arguments
        int i = 0;
        for (Method.Parameter p : c.parameters) {
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

    public void invoke(Compiler.MethodCompiler compiler, Sources sources, Map<String, Variable> variables, Variable variable, String methodName, List<Argument> args, Context context) {
        HashMap<String, Argument> argsByName = new HashMap<>();

        // put the name instead of the arguments
        Argument methodNameArg = Argument.of(methodName);
        argsByName.put("methodName", methodNameArg);

        variable.methodAllocations.push(new HashMap<>());

        for (Statement s : invokeMethod.body) {
            if (argStatement != s) {
                s.compile(compiler, sources, variable, argsByName, context);
                continue;
            }
            // For each argument, invoke the argMethod
            int argIndex = 0;
            HashMap<String, Argument> argArgs = new HashMap<>();
            for (Argument arg : args) {
                argArgs.put(argMethod.parameters.get(0).name, arg);
                argArgs.put("index", Argument.of(argIndex++));
                argArgs.put("methodName", methodNameArg);
                for (Statement as : argMethod.body) {
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
                case LG -> variable.documents.get(input);
                case AG -> {
                    String[] split = input.split("\\.");
                    Variable var = arguments.get(split[0]).variable;
                    yield var.documents.get(split[1]);
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
            if (param.equals(argVariable.usable.name())) {
                new InstructionStatement("m", "COPY", "TII", "LDA", "frameDataAddr", "ADA", "a", "ADS", "a").compile(compiler, sources, variable, arguments, context);
                new InstructionStatement("i","ADD", "TI", "LDA", "frameDataAddr", "LDA", "frameDataAddr", "ADS", "a").compile(compiler, sources, variable, arguments, context);
            } else if (param.equals("Pointer")) {
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
