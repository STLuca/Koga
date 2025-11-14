package language.enumeration;

import language.core.*;
import language.core.Structure;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
    ArrayList<Structure.GenericArgument> generics = new ArrayList<>();
    ArrayList<Argument> arguments = new ArrayList<>();

    void handle(
            Compiler.MethodCompiler compiler,
            Sources sources,
            Scope scope
    ) {
        ArrayList<String> argNames = new ArrayList<>();

        for (Argument arg : arguments) {
            if (arg.literal != null) {
                int literal = parseLiteral(arg.literal);
                String anonName = UUID.randomUUID().toString();

                scope.literals.put(anonName, literal);
                argNames.add(anonName);
            } else if (arg.block != null) {
                Block b = new Block(arg.block, sources, scope);
                String anonName = UUID.randomUUID().toString();

                scope.blocks.put(anonName, b);
                argNames.add(anonName);
            } else if (arg.name != null) {
                Scope variable = scope.findVariable(arg.name);
                if (variable == null) {
                    variable = scope.findVariable(arg.name);
                }

                argNames.add(arg.name);
            } else if (arg.array != null) {
                byte[] bytes = new byte[arg.array.size()];
                int i = 0;
                for (String literal : arg.array) {
                    bytes[i] = Byte.parseByte(literal);
                    i++;
                }
                int symbol = compiler.constant(bytes);
                String anonName = UUID.randomUUID().toString();

                scope.literals.put(anonName, symbol);
                argNames.add(anonName);
            }
        }

        if (!scope.defaults().isEmpty()) {
            argNames.addAll(scope.defaults());
        }

        ArrayList<String> oldGenerics = new ArrayList<>();
        for (Structure.GenericArgument g : generics) {
            oldGenerics.add(g.name);
        }

        switch (type) {
            case DECLARE -> {
                Structure structure = sources.structure(this.structure);
                structure.declare(compiler, sources, scope, variableName, generics);
            }
            case CONSTRUCT -> {
                Structure structure = sources.structure(this.structure);
                structure.construct(compiler, sources, scope, variableName, generics, methodName, argNames);
            }
            case INVOKE -> {
                Scope variable = scope.findVariable(variableName);
                Structure sc = variable.structure;
                sc.operate(compiler, sources, scope, variable, methodName, argNames);
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
            for (Statement stmt : block) {
                stmt.handle(compiler, sources, this.scope);
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
