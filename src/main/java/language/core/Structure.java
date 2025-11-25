package language.core;

import java.util.*;

public interface Structure {

    String name();
    int size(Repository repository);
    void declare(
            Compiler.MethodCompiler compiler,
            Repository repository,
            Scope scope,
            Scope.Generic descriptor,
            String name
    );
    void construct(
            Compiler.MethodCompiler compiler,
            Repository repository,
            Scope scope,
            Scope.Generic descriptor,
            String name,
            String constructorName,
            List<String> arguments
    );
    void operate(
            Compiler.MethodCompiler compiler,
            Repository repository,
            Scope scope,
            Scope variable,
            String operationName,
            List<String> arguments
    );

}