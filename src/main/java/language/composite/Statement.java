package language.composite;

import language.core.Compiler;
import language.core.Scope;
import language.core.Sources;

public interface Statement {

    void handle(
        Compiler.MethodCompiler compiler,
        Sources sources,
        String name,
        Scope scope
    );

}
