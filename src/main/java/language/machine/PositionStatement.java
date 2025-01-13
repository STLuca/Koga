package language.machine;

import language.core.Argument;
import language.core.Compiler;
import language.core.Context;
import language.core.Variable;

import java.util.Map;

public class PositionStatement implements Statement {

    String addr;
    String prevName;

    public void compile(Compiler.MethodCompiler compiler, Variable variable, Map<String, Argument> arguments, Context context) {
        int addr;
        if (variable.methodAllocations.peek().containsKey(this.addr)) {
            addr = variable.methodAllocations.peek().get(this.addr).location();
        } else {
            addr = variable.allocations.get(this.addr).location();
        }
        int prev = compiler.position(addr);
        if (this.prevName != null) {
            variable.methodAllocations.peek().put(prevName, new Variable.Allocation(4, prev));
        }
    }

}
