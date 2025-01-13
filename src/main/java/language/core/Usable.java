package language.core;

import java.util.*;

public interface Usable {

    String name();
    int size();
    void declare(Compiler.MethodCompiler compiler, Classes classes, Variable variable, List<String> generics);
    void proxy(Classes classes, Variable variable, int location);
    void construct(
            Compiler.MethodCompiler compiler,
            Classes classes,
            Map<String, Variable> variables,
            Variable variable,
            List<String> generics,
            String constructorName,
            List<Argument> arguments,
            Context context
    );
    void invoke(
            Compiler.MethodCompiler compiler,
            Map<String, Variable> variables,
            Variable variable,
            String methodName,
            List<Argument> arguments,
            Context context
    );

}