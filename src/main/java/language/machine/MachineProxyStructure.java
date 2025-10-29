package language.machine;

import language.core.*;
import language.core.Compiler;

import java.util.List;
import java.util.Map;

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
    
    public void declare(Compiler.MethodCompiler compiler, Sources sources, Map<String, Variable> variables, String name, List<String> generics) {
        Variable variable = new Variable();
        variable.name = name;
        variable.structure = sources.structure(generics.get(0));
        variables.put(name, variable);
        // variable.proxy = true
    }
    
    public void proxy(Sources sources, Variable variable, int location) {
        throw new RuntimeException("Not supported");
    }
    
    public void construct(Compiler.MethodCompiler compiler, Sources sources, Map<String, Variable> variables, String name, List<String> generics, String constructorName, List<Argument> arguments, Context context) {
        // init the proxy
        // compiler.proxy(variable.name, null);
        // First generic is the proxied class
        // variable.generics.get(0).declare(compiler, variable, List.of(), null);
    }
    
    public void operate(Compiler.MethodCompiler compiler, Sources sources, Map<String, Variable> variables, Variable variable, String operationName, List<Argument> arguments, Context context) {
        // variable.generics.get(0).invoke(compiler, variable, admin, methodName, arguments);
    }

}
