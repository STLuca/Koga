package language.core;

import java.util.*;

public interface Structure {

    String name();
    default int genericIndex(String name) {
        return -1;
    };
    int size(Sources sources);
    void proxy(Sources sources, Variable variable, int location);
    void declare(
            Compiler.MethodCompiler compiler,
            Sources sources,
            Context context,
            String name,
            List<String> generics
    );
    void construct(
            Compiler.MethodCompiler compiler,
            Sources sources,
            Context context,
            String name,
            List<String> generics,
            String constructorName,
            List<Argument> arguments
    );
    void operate(
            Compiler.MethodCompiler compiler,
            Sources sources,
            Context context,
            Variable variable,
            String operationName,
            List<Argument> arguments
    );

}