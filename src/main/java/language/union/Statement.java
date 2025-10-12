package language.union;

import language.core.*;

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
    String usable;
    String variableName;
    String methodName;
    ArrayList<String> generics = new ArrayList<>();
    ArrayList<Argument> arguments = new ArrayList<>();

    void handle(
            Compiler.MethodCompiler compiler,
            Sources sources,
            Map<String, Variable> variables,
            Map<String, language.core.Argument> argsByName,
            String name,
            Context context
    ) {
        ArrayList<language.core.Argument> args = new ArrayList<>();

        for (Argument arg : arguments) {
            if (argsByName.containsKey(arg.name)) {
                args.add(argsByName.get(arg.name));
            } else if (arg.literal != null) {
                int literal = parseLiteral(arg.literal);
                args.add(language.core.Argument.of(literal));
            } else if (arg.block != null) {
                Block b = new Block(arg.block, sources, variables, argsByName, name, context);
                args.add(language.core.Argument.of(b));
            } else if (arg.name != null) {
                Variable variable = variables.get(name + "." + arg.name);
                if (variable == null) {
                    variable = variables.get(arg.name);
                }
                if (variable != null) {
                    args.add(language.core.Argument.of(variable));
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

        if (!context.defaults().isEmpty()) {
            args.addAll(context.defaults());
        }

        switch (type) {
            case DECLARE -> {
                Usable usable = sources.usable(this.usable);
                usable.declare(compiler, sources, variables, name + "." + variableName, generics);
            }
            case CONSTRUCT -> {
                Usable usable = sources.usable(this.usable);
                usable.construct(compiler, sources, variables, name + "." + variableName, generics, methodName, args, context);
            }
            case INVOKE -> {
                Variable variable = variables.get(name + "." + variableName);
                if (variable == null) {
                    variable = argsByName.get(variableName).variable;
                }
                Usable sc = variable.usable;
                sc.invoke(compiler, sources, variables, variable, methodName, args, context);
            }
        }
    }

    static class Block implements language.core.Block {

        List<Statement> block;
        Sources sources;
        Map<String, Variable> variables;
        Map<String, language.core.Argument> argsByName;
        String name;
        Context context;

        public Block(
                List<Statement> block,
                Sources sources,
                Map<String, Variable> variables,
                Map<String, language.core.Argument> argsByName,
                String name,
                Context context
        ) {
            this.block = block;
            this.sources = sources;
            this.variables = variables;
            this.argsByName = argsByName;
            this.name = name;
            this.context = context;
        }
        
        public void execute(Compiler.MethodCompiler compiler) {
            for (Statement stmt : block) {
                stmt.handle(compiler, sources, variables, argsByName, name, context);
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
