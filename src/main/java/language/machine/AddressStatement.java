package language.machine;

import language.core.*;

public class AddressStatement implements Statement {

    String name;
    
    public void compile(Compiler.MethodCompiler compiler, Repository repository, Scope variable, Scope scope) {
        Scope.Allocation allocation = scope.findAllocation(name).orElse(null);
        if (allocation != null) {
            compiler.address(allocation.location());
            return;
        }
        Scope.Allocation variableAllocation = variable.findAllocation(name).orElse(null);
        if (variableAllocation != null) {
            compiler.address(variableAllocation.location());
            return;
        }
        int address = compiler.address();
        Scope.Allocation newAllocation = new Scope.Allocation(4, address);
        scope.add(name, newAllocation);
    }

}
