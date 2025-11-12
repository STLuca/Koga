package language.composite;

import language.core.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
            Map<String, language.core.Argument> argsByName,
            Map<String, Scope.Generic> genericsByName,
            String name,
            Scope scope
    ) {
        ArrayList<language.core.Argument> args = new ArrayList<>();

        for (Argument arg : arguments) {
            if (argsByName.containsKey(arg.name)) {
                args.add(argsByName.get(arg.name));
            } else if (arg.literal != null) {
                int literal = parseLiteral(arg.literal);
                args.add(language.core.Argument.of(literal));
            } else if (arg.block != null) {
                Block b = new Block(arg.block, sources, argsByName, genericsByName, name, scope);
                args.add(language.core.Argument.of(b));
            } else if (arg.name != null) {
                Scope variable = scope.findVariable(arg.name);
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

        if (!scope.defaults().isEmpty()) {
            args.addAll(scope.defaults());
        }

        ArrayList<String> resolvedGenerics = new ArrayList<>();
        for (GenericArgument g : this.generics) {
            switch (g.type) {
                case Known -> {
                    resolvedGenerics.add(g.name);
                }
                case Generic -> {
                    String resolved = genericsByName.get(g.name).structure.name();
                    resolvedGenerics.add(resolved);
                }
                case null, default -> {
                    throw new RuntimeException();
                }
            }
        }

        switch (type) {
            case DECLARE -> {
                if (genericsByName.containsKey(this.structure)) {
                    Structure structure = genericsByName.get(this.structure).structure;
                    structure.declare(compiler, sources, scope, variableName, resolvedGenerics, null);
                } else {
                    Structure structure = sources.structure(this.structure);
                    structure.declare(compiler, sources, scope, variableName, resolvedGenerics, null);
                }
            }
            case CONSTRUCT -> {
                Structure structure = sources.structure(this.structure);
                structure.construct(compiler, sources, scope, variableName, resolvedGenerics, null, methodName, args);
            }
            case INVOKE -> {
                Scope variable = scope.findVariable(variableName);
                if (variable == null) {
                    variable = argsByName.get(variableName).variable;
                }
                Structure sc = variable.structure;
                sc.operate(compiler, sources, scope, variable, methodName, args);
            }
        }
    }

    static class Block implements language.core.Block {

        List<Statement> block;
        Sources sources;
        Map<String, language.core.Argument> argsByName;
        Map<String, Scope.Generic> genericsByName;
        String name;
        Scope scope;

        public Block(
                List<Statement> block,
                Sources sources,
                Map<String, language.core.Argument> argsByName,
                Map<String, Scope.Generic> genericsByName,
                String name,
                Scope scope
        ) {
            this.block = block;
            this.sources = sources;
            this.argsByName = argsByName;
            this.genericsByName = genericsByName;
            this.name = name;
            this.scope = scope;
        }
        
        public void execute(Compiler.MethodCompiler compiler, Scope scope) {
            this.scope.scopes.putAll(scope.implicit);
            for (Statement stmt : block) {
                stmt.handle(compiler, sources, argsByName, genericsByName, name, this.scope);
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
