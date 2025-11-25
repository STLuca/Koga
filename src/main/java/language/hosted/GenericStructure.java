package language.hosted;

import language.core.Compiler;
import language.core.Repository;
import language.core.Scope;
import language.core.Structure;

import java.util.List;

public class GenericStructure implements Structure {

    String name;

    @Override
    public String name() {
        return name;
    }

    @Override
    public int size(Repository repository) {
        throw new RuntimeException();
    }

    @Override
    public void declare(Compiler.MethodCompiler compiler, Repository repository, Scope scope, Scope.Generic descriptor, String name) {
        throw new RuntimeException();
    }

    @Override
    public void construct(Compiler.MethodCompiler compiler, Repository repository, Scope scope, Scope.Generic descriptor, String name, String constructorName, List<String> arguments) {
        throw new RuntimeException();
    }

    @Override
    public void operate(Compiler.MethodCompiler compiler, Repository repository, Scope scope, Scope variable, String operationName, List<String> arguments) {
        throw new RuntimeException();
    }

}
