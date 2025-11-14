package language.composite;

import language.core.Compiler;
import language.core.Scope;
import language.core.Repository;

public interface Statement {

    void handle(
        Compiler.MethodCompiler compiler,
        Repository repository,
        String name,
        Scope scope
    );

}
