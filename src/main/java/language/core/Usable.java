package language.core;

import java.util.*;

public interface Usable {

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
    void invoke(
            Compiler.MethodCompiler compiler,
            Sources sources,
            Map<String, Variable> variables,
            Variable variable,
            String methodName,
            List<Argument> arguments,
            Context context
    );

}