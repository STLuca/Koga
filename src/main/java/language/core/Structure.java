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
            Map<String, Variable> variables,
            String name,
            List<String> generics
    );
    void construct(
            Compiler.MethodCompiler compiler,
            Sources sources,
            Map<String, Variable> variables,
            String name,
            List<String> generics,
            String constructorName,
            List<Argument> arguments,
            Context context
    );
    void operate(
            Compiler.MethodCompiler compiler,
            Sources sources,
            Map<String, Variable> variables,
            Variable variable,
            String operationName,
            List<Argument> arguments,
            Context context
    );

}