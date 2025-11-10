package language.composite;

import language.core.Argument;
import language.core.Compiler;
import language.core.Scope;
import language.core.Sources;

import java.util.Map;

public class ScopeStatement implements Statement {

    enum Type {
        Implicit
    }

    Type type;
    String name;

    @Override
    public void handle(Compiler.MethodCompiler compiler, Sources sources, Map<String, Argument> argsByName, Map<String, Scope.Generic> genericsByName, String name, Scope scope) {
        switch (type) {
            case Implicit -> {
                scope.implicit.put(this.name, scope.scopes.get(this.name));
            }
            case null, default -> {
                throw new RuntimeException();
            }
        }
    }
}
