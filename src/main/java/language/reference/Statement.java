package language.reference;

import language.core.*;
import language.core.Compiler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Statement {

    public enum Type {
        DECLARE,
        CONSTRUCT,
        INVOKE
    }

    public static class Argument {
        String literal;
        String name;
        List<Statement> block;
        List<String> array;
    }

    Type type;
    String clazz;
    String variableName;
    String methodName;
    List<String> generics = new ArrayList<>();
    List<Argument> arguments = new ArrayList<>();

    void handle(
            Compiler.MethodCompiler compiler,
            Classes classes,
            Map<String, Variable> variables,
            Context context
    ) {
        List<language.core.Argument> args = new ArrayList<>();

        for (Argument arg : arguments) {
            if (arg.literal != null) {
                int literal = parseLiteral(arg.literal);
                args.add(language.core.Argument.of(literal));
            } else if (arg.block != null) {
                Block b = new Block(arg.block, classes, variables, context);
                args.add(language.core.Argument.of(b));
            } else if (arg.name != null) {
                if (variables.containsKey(arg.name)) {
                    Variable v = variables.get(arg.name);
                    args.add(language.core.Argument.of(v));
                } else {
                    args.add(language.core.Argument.of(arg.name));
                }
            } else if (arg.array != null) {
                byte[] bytes = new byte[arg.array.size()];
                int i = 0;
                for (String literal : arg.array) {
                    bytes[i] = Byte.parseByte(literal);
                    i++;
                }
                int symbol = compiler.constant(bytes);
                args.add(language.core.Argument.of(symbol));
            }
        }

        switch (type) {
            case DECLARE -> {
                Variable variable = new Variable();
                variable.name = variableName;
                variable.clazz = classes.usable(clazz);
                variable.clazz.declare(compiler, classes, variable, generics);
                variables.put(variableName, variable);
            }
            case CONSTRUCT -> {
                Variable variable = new Variable();
                variable.name = variableName;
                variable.clazz = classes.usable(clazz);
                variables.put(variableName, variable);
                variable.clazz.construct(compiler, classes, variables, variable, generics, methodName, args, context);
            }
            case INVOKE -> {
                Variable variable = variables.get(variableName);
                Usable sc = variable.clazz;
                sc.invoke(compiler, variables, variable, methodName, args, context);
            }
        }
    }

    static class Block implements language.core.Block {

        List<Statement> block;
        Classes classes;
        Map<String, Variable> variables;
        Context context;

        public Block(
                List<Statement> block,
                Classes classes,
                Map<String, Variable> variables,
                Context context
        ) {
            this.block = block;
            this.classes = classes;
            this.variables = variables;
            this.context = context;
        }

        
        public void execute(Compiler.MethodCompiler compiler) {
            for (Statement stmt : block) {
                stmt.handle(compiler, classes, variables, context);
            }
        }
    }

    int parseLiteral(String v) {
        if (v.startsWith("0d")) {
            return Integer.parseInt(v.substring(2));
        }
        if (v.equals("true")) return 1;
        if (v.equals("false")) return 0;
        if (v.matches("'[a-zA-Z]'")) return v.charAt(1);
        try {
            return Integer.parseInt(v);
        } catch (RuntimeException ignored) {}
        throw new RuntimeException("Not implemented");
    }

}
