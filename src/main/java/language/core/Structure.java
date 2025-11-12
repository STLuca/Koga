package language.core;

import java.util.*;

public interface Structure {

    class GenericArgument {
        public String name;
        public ArrayList<GenericArgument> generics = new ArrayList<>();
    }

    String name();
    int size(Sources sources);
    void proxy(Sources sources, Scope variable, int location);
    void declare(
            Compiler.MethodCompiler compiler,
            Sources sources,
            Scope scope,
            String name,
            List<String> generics,
            List<GenericArgument> nestedGenerics
    );
    void construct(
            Compiler.MethodCompiler compiler,
            Sources sources,
            Scope scope,
            String name,
            List<String> generics,
            List<GenericArgument> nestedGenerics,
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