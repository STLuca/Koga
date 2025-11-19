package language.machine;

import language.core.*;

public class AddressStatement implements Statement {

    String name;
    
    public void compile(Compiler.MethodCompiler compiler, Repository repository, Scope variable, Scope scope) {
        Scope.Allocation allocation = scope.findAllocation(name).orElse(null);
        if (allocation != null) {
            compiler.address(allocation.location());
            return;
        } else {
            Integer address = scope.findAddress(name).orElse(null);
            if (address != null) {
                compiler.address(address);
                return;
            }
        }
        int address = compiler.address();
        Scope.Allocation newAllocation = new Scope.Allocation(4, address);
        scope.put(name, newAllocation);
        scope.putAddress(name, address);
    }

}
