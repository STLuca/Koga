package language.machine;

import language.core.*;

import java.util.ArrayList;
import java.util.Map;

public class ProxyStatement implements Statement {

    ArrayList<String> arguments = new ArrayList<>();
    
    public void compile(Compiler.MethodCompiler compiler, Sources sources, Context.Scope variable, Map<String, Argument> arguments, Context context) {
        if (arguments.get(this.arguments.get(0)).type != Argument.Type.Variable) throw new RuntimeException();
        if (!variable.allocations.containsKey(this.arguments.get(1))) throw new RuntimeException();

        Context.Scope proxy = arguments.get(this.arguments.get(0)).variable;
        int location = variable.allocations.get(this.arguments.get(1)).location();
        proxy.structure.proxy(null, proxy, location);
    }
}
