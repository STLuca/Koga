package language.core;

import java.util.*;

public interface Structure {

    String name();
    int size(Sources sources);
    void proxy(Sources sources, Context.Scope variable, int location);
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
            Context.Scope variable,
            String operationName,
            List<Argument> arguments
    );

}