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
    int size(Repository repository);
    void declare(
            Compiler.MethodCompiler compiler,
            Repository repository,
            Scope scope,
            String name,
            List<GenericArgument> generics
    );
    void construct(
            Compiler.MethodCompiler compiler,
            Repository repository,
            Scope scope,
            String name,
            List<GenericArgument> generics,
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