package language.machine;

import language.core.*;
import language.core.Compiler;

import java.util.List;

public class MachineProxyStructure implements Structure {

    public final static String NAME = "core.Proxy";
    public static MachineProxyStructure INSTANCE = new MachineProxyStructure();

    MachineProxyStructure() {}
    
    public String name() {
        return NAME;
    }
    
    public int size(Repository repository) {
        return 0;
    }
    
    public void declare(Compiler.MethodCompiler compiler, Repository repository, Scope scope, String name, List<GenericArgument> generics, Scope.Generic descriptor) {
        Scope variable = scope.state(repository.structure(generics.get(0).name), name);
        // variable.proxy = true
    }
    
    public void construct(Compiler.MethodCompiler compiler, Repository repository, Scope scope, String name, List<GenericArgument> generics, String constructorName, List<String> argumentNames, Scope.Generic descriptor) {
        // init the proxy
        // compiler.proxy(variable.name, null);
        // First generic is the proxied class
        // variable.generics.get(0).declare(compiler, variable, List.of(), null);
    }
    
    public void operate(Compiler.MethodCompiler compiler, Repository repository, Scope scope, Scope variable, String operationName, List<String> arguments) {
        // variable.generics.get(0).invoke(compiler, variable, admin, methodName, arguments);
    }

}
