package language.machine;

import language.core.*;

import java.util.Map;

public class PositionStatement implements Statement {

    String addr;
    String prevName;

    public void compile(Compiler.MethodCompiler compiler, Sources sources, Context.Scope variable, Map<String, Argument> arguments, Context context) {
        int addr;
        if (context.findAllocation(this.addr) != null) {
            addr = context.findAllocation(this.addr).location();
        } else {
            addr = variable.allocations.get(this.addr).location();
        }
        int prev = compiler.position(addr);
        if (this.prevName != null) {
            Context.Allocation allocation = new Context.Allocation(4, prev);
            context.add(prevName, allocation);
        }
    }

}
