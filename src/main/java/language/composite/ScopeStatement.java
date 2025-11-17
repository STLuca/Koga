package language.composite;

import language.core.Compiler;
import language.core.Repository;
import language.core.Scope;

public class ScopeStatement implements Statement {

    enum Type {
        Implicit
    }

    Type type;
    String name;

    @Override
    public void handle(Compiler.MethodCompiler compiler, Repository repository, Scope scope) {
        switch (type) {
            case Implicit -> {
                scope.implicit().put(this.name, scope.findVariable(this.name).orElseThrow());
            }
            case null, default -> {
                throw new RuntimeException();
            }
        }
    }
}
