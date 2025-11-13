package language.machine;

import language.core.*;

public class PositionStatement implements Statement {

    String addr;
    String prevName;

    public void compile(Compiler.MethodCompiler compiler, Sources sources, Scope variable, Scope scope) {
        int addr;
        if (scope.findAllocation(this.addr) != null) {
            addr = scope.findAllocation(this.addr).location();
        } else {
            addr = variable.allocations.get(this.addr).location();
        }
        int prev = compiler.position(addr);
        if (this.prevName != null) {
            Scope.Allocation allocation = new Scope.Allocation(4, prev);
            scope.add(prevName, allocation);
        }
    }

}
