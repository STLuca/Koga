package language.machine;

import language.core.*;
import language.core.Compiler;

import java.util.List;
import java.util.Map;

public class MachineProxyUsable implements Usable {

    public final static String NAME = "Proxy";
    public static MachineProxyUsable INSTANCE = new MachineProxyUsable();

    MachineProxyUsable() {}

    
    public String name() {
        return NAME;
    }

    
    public int size() {
        return 0;
    }

    
    public void declare(Compiler.MethodCompiler compiler, Classes classes, Variable variable, List<String> generics) {
        variable.clazz = classes.usable(generics.get(0));
        // variable.proxy = true
    }

    
    public void proxy(Classes classes, Variable variable, int location) {
        throw new RuntimeException("Not supported");
    }

    
    public void construct(Compiler.MethodCompiler compiler, Classes classes, Map<String, Variable> variables, Variable variable, List<String> generics, String constructorName, List<Argument> arguments) {
        // init the proxy
        // compiler.proxy(variable.name, null);
        // First generic is the proxied class
        // variable.generics.get(0).declare(compiler, variable, List.of(), null);
    }

    
    public void invoke(Compiler.MethodCompiler compiler, Map<String, Variable> variables, Variable variable, String methodName, List<Argument> arguments) {
        // variable.generics.get(0).invoke(compiler, variable, admin, methodName, arguments);
    }

}
