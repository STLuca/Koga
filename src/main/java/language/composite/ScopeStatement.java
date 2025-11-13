package language.composite;

import language.core.Compiler;
import language.core.Scope;
import language.core.Sources;

public class ScopeStatement implements Statement {

    enum Type {
        Implicit
    }

    Type type;
    String name;

    @Override
    public void handle(Compiler.MethodCompiler compiler, Sources sources, String name, Scope scope) {
        switch (type) {
            case Implicit -> {
                scope.implicitScope.scopes.put(this.name, scope.scopes.get(this.name));
            }
            case null, default -> {
                throw new RuntimeException();
            }
        }
    }
}
