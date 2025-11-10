package language.composite;

import language.core.Argument;
import language.core.Compiler;
import language.core.Scope;
import language.core.Sources;

import java.util.Map;

public interface Statement {

    void handle(
            Compiler.MethodCompiler compiler,
            Sources sources,
            Map<String, Argument> argsByName,
            Map<String, Scope.Generic> genericsByName,
            String name,
            Scope scope
    );

}
