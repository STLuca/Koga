package language.machine;

import language.core.*;

import java.util.ArrayList;

public class ProxyStatement implements Statement {

    ArrayList<String> arguments = new ArrayList<>();
    
    public void compile(Compiler.MethodCompiler compiler, Repository repository, Scope variable, Scope scope) {
        Scope proxy = scope.findVariable(this.arguments.get(0)).orElseThrow();
        Scope.Allocation allocation = variable.findAllocation(this.arguments.get(1)).orElseThrow();

        int location = allocation.location();

        // proxy.structure().proxy(repository, proxy, location);
    }
}
