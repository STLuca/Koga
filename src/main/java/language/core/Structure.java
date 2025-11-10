package language.core;

import java.util.*;

public interface Structure {

    String name();
    int size(Sources sources);
    void proxy(Sources sources, Scope variable, int location);
    void declare(
            Compiler.MethodCompiler compiler,
            Sources sources,
            Scope scope,
            String name,
            List<String> generics
    );
    void construct(
            Compiler.MethodCompiler compiler,
            Sources sources,
            Scope scope,
            String name,
            List<String> generics,
            String constructorName,
            List<Argument> arguments
    );
    void operate(
            Compiler.MethodCompiler compiler,
            Sources sources,
            Scope scope,
            Scope variable,
            String operationName,
            List<Argument> arguments
    );

}