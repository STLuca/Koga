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
    
    public int size(Sources sources) {
        return 0;
    }
    
    public void declare(Compiler.MethodCompiler compiler, Sources sources, Scope scope, String name, List<String> generics, List<GenericArgument> nestedGenerics) {
        Scope variable = scope.add(name);
        variable.name = name;
        variable.structure = sources.structure(generics.get(0));
        // variable.proxy = true
    }
    
    public void proxy(Sources sources, Scope variable, int location) {
        throw new RuntimeException("Not supported");
    }
    
    public void construct(Compiler.MethodCompiler compiler, Sources sources, Scope scope, String name, List<String> generics, List<GenericArgument> nestedGenerics, String constructorName, List<String> argumentNames) {
        // init the proxy
        // compiler.proxy(variable.name, null);
        // First generic is the proxied class
        // variable.generics.get(0).declare(compiler, variable, List.of(), null);
    }
    
    public void operate(Compiler.MethodCompiler compiler, Sources sources, Scope scope, Scope variable, String operationName, List<String> arguments) {
        // variable.generics.get(0).invoke(compiler, variable, admin, methodName, arguments);
    }

}
