package language.machine;

import language.core.*;

public class PositionStatement implements Statement {

    String addr;
    String prevName;

    public void compile(Compiler.MethodCompiler compiler, Repository repository, Scope variable, Scope scope) {
        int addr;
        Scope.Allocation allocation = scope.findAllocation(this.addr).orElse(null);
        if (allocation != null) {
            addr = allocation.location();
        } else {
            Scope.Allocation varAllocation = variable.findAllocation(this.addr).orElseThrow();
            addr = varAllocation.location();
        }
        int prev = compiler.position(addr);
        if (this.prevName != null) {
            Scope.Allocation newAllocation = new Scope.Allocation(4, prev);
            scope.add(prevName, newAllocation);
        }
    }

}
