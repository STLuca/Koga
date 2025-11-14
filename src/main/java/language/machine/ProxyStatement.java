package language.machine;

import language.core.*;

import java.util.ArrayList;

public class ProxyStatement implements Statement {

    ArrayList<String> arguments = new ArrayList<>();
    
    public void compile(Compiler.MethodCompiler compiler, Repository repository, Scope variable, Scope scope) {
        Scope proxy = scope.findVariable(this.arguments.get(0));
        if (proxy == null) throw new RuntimeException();
        if (!variable.allocations.containsKey(this.arguments.get(1))) throw new RuntimeException();

        int location = variable.allocations.get(this.arguments.get(1)).location();
        proxy.structure.proxy(null, proxy, location);
    }
}
