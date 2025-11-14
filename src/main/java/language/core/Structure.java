package language.core;

import java.util.*;

public interface Structure {

    class GenericArgument {
        public enum Type { Known, Unknown }
        public Type type;
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
            List<GenericArgument> generics
    );
    void construct(
            Compiler.MethodCompiler compiler,
            Sources sources,
            Scope scope,
            String name,
            List<GenericArgument> generics,
            String constructorName,
            List<String> arguments
    );
    void operate(
            Compiler.MethodCompiler compiler,
            Sources sources,
            Scope scope,
            Scope variable,
            String operationName,
            List<String> arguments
    );

}