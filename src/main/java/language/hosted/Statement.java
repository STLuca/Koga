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
            Scope scope
    ) {
        ArrayList<language.core.Argument> args = new ArrayList<>();

        for (Argument arg : arguments) {
            if (arg.literal != null) {
                int literal = parseLiteral(arg.literal);
                args.add(language.core.Argument.of(literal));
            } else if (arg.block != null) {
                Block b = new Block(arg.block, sources, scope);
                args.add(language.core.Argument.of(b));
            } else if (arg.name != null) {
                Scope v = scope.findVariable(arg.name);
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

        if (!scope.defaults().isEmpty()) {
            args.addAll(scope.defaults());
        }

        switch (type) {
            case DECLARE -> {
                Structure structure = sources.structure(this.structure);
                structure.declare(compiler, sources, scope, variableName, generics);
            }
            case CONSTRUCT -> {
                Structure structure = sources.structure(this.structure);
                structure.construct(compiler, sources, scope, variableName, generics, methodName, args);
            }
            case INVOKE -> {
                Scope variable = scope.findVariable(variableName);
                Structure sc = variable.structure;
                sc.operate(compiler, sources, scope, variable, methodName, args);
            }
        }
    }

    static class Block implements language.core.Block {

        List<Statement> block;
        Sources sources;
        Scope scope;

        public Block(
                List<Statement> block,
                Sources sources,
                Scope scope
        ) {
            this.block = block;
            this.sources = sources;
            this.scope = scope;
        }
        
        public void execute(Compiler.MethodCompiler compiler, Scope scope) {
            this.scope.scopes.putAll(scope.implicit);
            for (Statement stmt : block) {
                stmt.handle(compiler, sources, this.scope);
            }
            for (String key : scope.implicit.keySet()) {
                this.scope.scopes.remove(key);
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
