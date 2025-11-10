package language.machine;

import language.core.*;

import java.util.Map;

public class AddressStatement implements Statement {

    String name;
    
    public void compile(Compiler.MethodCompiler compiler, Sources sources, Scope variable, Map<String, Argument> arguments, Scope scope) {
        if (scope.findAllocation(name) != null) {
            compiler.address(scope.findAllocation(name).location());
            return;
        }
        if (variable.allocations.containsKey(name)) {
            compiler.address(variable.allocations.get(name).location());
            return;
        }
        int address = compiler.address();
        Scope.Allocation allocation = new Scope.Allocation(4, address);
        scope.add(name, allocation);
    }

}
