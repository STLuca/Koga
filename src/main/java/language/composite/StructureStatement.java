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

    Type type;
    String structure;
    String variableName;
    String methodName;
    ArrayList<Structure.GenericArgument> generics = new ArrayList<>();
    ArrayList<Argument> arguments = new ArrayList<>();

    public void handle(
            Compiler.MethodCompiler compiler,
            Repository repository,
            Scope scope
    ) {
        ArrayList<String> argNames = new ArrayList<>();

        for (Argument arg : arguments) {
            if (arg.literal != null) {
                int literal = parseLiteral(arg.literal);
                String anonName = UUID.randomUUID().toString();

                scope.put(anonName, literal);
                argNames.add(anonName);
            } else if (arg.block != null) {
                Block b = new Block(arg.block, repository, scope);
                String anonName = UUID.randomUUID().toString();

                scope.put(anonName, b);
                argNames.add(anonName);
            } else if (arg.name != null) {
                Scope variable = scope.findVariable(arg.name).orElse(null);
                if (variable != null) {
                    scope.put(arg.name, variable);
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

                scope.put(anonName, symbol);
                argNames.add(anonName);
            }
        }

        if (!scope.defaults().isEmpty()) {
            argNames.addAll(scope.defaults());
        }

        switch (type) {
            case DECLARE -> {
                Scope.Generic g = scope.findGeneric(this.structure).orElse(null);
                if (g != null) {
                    Structure structure = g.structure;
                    structure.declare(compiler, repository, scope, variableName, generics);
                } else {
                    Structure structure = repository.structure(this.structure);
                    structure.declare(compiler, repository, scope, variableName, generics);
                }
            }
            case CONSTRUCT -> {
                Structure structure = repository.structure(this.structure);
                structure.construct(compiler, repository, scope, variableName, generics, methodName, argNames);
            }
            case INVOKE -> {
                Scope variable = scope.findVariable(variableName).orElseThrow();
                Structure sc = variable.structure();
                sc.operate(compiler, repository, scope, variable, methodName, argNames);
            }
        }
    }

    static class Block implements Scope.Block {

        List<Statement> block;
        Repository repository;
        Scope scope;

        public Block(
                List<Statement> block,
                Repository repository,
                Scope scope
        ) {
            this.block = block;
            this.repository = repository;
            this.scope = scope;
        }
        
        public void execute(Compiler.MethodCompiler compiler, Scope scope) {
            Scope blockScope = scope.startOperation(this.scope, "block");
            for (Statement stmt : block) {
                stmt.handle(compiler, repository, blockScope);
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
