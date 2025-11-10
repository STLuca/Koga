package language.hosted;

import language.core.*;
import language.core.Compiler;

import java.util.ArrayList;
import java.util.List;

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
    String structure;
    String variableName;
    String methodName;
    ArrayList<String> generics = new ArrayList<>();
    ArrayList<Argument> arguments = new ArrayList<>();

    void handle(
            Compiler.MethodCompiler compiler,
            Sources sources,
            Context context
    ) {
        ArrayList<language.core.Argument> args = new ArrayList<>();

        for (Argument arg : arguments) {
            if (arg.literal != null) {
                int literal = parseLiteral(arg.literal);
                args.add(language.core.Argument.of(literal));
            } else if (arg.block != null) {
                Block b = new Block(arg.block, sources, context);
                args.add(language.core.Argument.of(b));
            } else if (arg.name != null) {
                Context.Scope v = context.findVariable(arg.name);
                if (v != null) {
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

        if (!context.defaults().isEmpty()) {
            args.addAll(context.defaults());
        }

        switch (type) {
            case DECLARE -> {
                Structure structure = sources.structure(this.structure);
                structure.declare(compiler, sources, context, variableName, generics);
            }
            case CONSTRUCT -> {
                Structure structure = sources.structure(this.structure);
                structure.construct(compiler, sources, context, variableName, generics, methodName, args);
            }
            case INVOKE -> {
                Context.Scope variable = context.findVariable(variableName);
                Structure sc = variable.structure;
                sc.operate(compiler, sources, context, variable, methodName, args);
            }
        }
    }

    static class Block implements language.core.Block {

        List<Statement> block;
        Sources sources;
        Context context;

        public Block(
                List<Statement> block,
                Sources sources,
                Context context
        ) {
            this.block = block;
            this.sources = sources;
            this.context = context;
        }
        
        public void execute(Compiler.MethodCompiler compiler) {
            for (Statement stmt : block) {
                stmt.handle(compiler, sources, context);
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
