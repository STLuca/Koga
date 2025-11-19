package language.machine;

import language.core.*;

public class PositionStatement implements Statement {

    String addr;
    String prevName;

    public void compile(Compiler.MethodCompiler compiler, Repository repository, Scope variable, Scope scope) {
        Scope.Allocation allocation = scope.findAllocation(this.addr).orElseThrow();
        int addr = allocation.location();
        int prev = compiler.position(addr);
        if (this.prevName != null) {
            Scope.Allocation newAllocation = new Scope.Allocation(4, prev);
            scope.put(prevName, newAllocation);
        }
    }

}
