package language.composite;

import language.core.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class StructureStatement implements Statement {

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

    public static class GenericArgument {
        enum Type {
            Known,
            Generic
        }
        Type type;
        String name;
    }

    Type type;
    String structure;
    String variableName;
    String methodName;
    ArrayList<GenericArgument> generics = new ArrayList<>();
    ArrayList<Argument> arguments = new ArrayList<>();

    public void handle(
            Compiler.MethodCompiler compiler,
            Sources sources,
            String name,
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
                Block b = new Block(arg.block, sources, name, scope);
                String anonName = UUID.randomUUID().toString();

                scope.blocks.put(anonName, b);
                argNames.add(anonName);
            } else if (arg.name != null) {
                Scope variable = scope.findVariable(arg.name);
                if (variable != null) {
                    scope.scopes.put(arg.name, variable);
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

        ArrayList<String> resolvedGenerics = new ArrayList<>();
        for (GenericArgument g : this.generics) {
            switch (g.type) {
                case Known -> {
                    resolvedGenerics.add(g.name);
                }
                case Generic -> {
                    String resolved = scope.parent.generics.get(g.name).structure.name();
                    resolvedGenerics.add(resolved);
                }
                case null, default -> {
                    throw new RuntimeException();
                }
            }
        }

        switch (type) {
            case DECLARE -> {
                Scope.Generic g = scope.parent.generics.get(this.structure);
                if (g != null) {
                    Structure structure = g.structure;
                    structure.declare(compiler, sources, scope, variableName, resolvedGenerics, null);
                } else {
                    Structure structure = sources.structure(this.structure);
                    structure.declare(compiler, sources, scope, variableName, resolvedGenerics, null);
                }
            }
            case CONSTRUCT -> {
                Structure structure = sources.structure(this.structure);
                structure.construct(compiler, sources, scope, variableName, resolvedGenerics, null, methodName, argNames);
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
        String name;
        Scope scope;

        public Block(
                List<Statement> block,
                Sources sources,
                String name,
                Scope scope
        ) {
            this.block = block;
            this.sources = sources;
            this.name = name;
            this.scope = scope;
        }
        
        public void execute(Compiler.MethodCompiler compiler, Scope scope) {
            this.scope.addImplicit(scope.implicitScope);
            for (Statement stmt : block) {
                stmt.handle(compiler, sources, name, this.scope);
            }
            this.scope.removeImplicit(scope.implicitScope);
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
