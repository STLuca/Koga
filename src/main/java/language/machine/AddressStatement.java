package language.machine;

import language.core.*;

import java.util.Map;

public class AddressStatement implements Statement {

    String name;
    
    public void compile(Compiler.MethodCompiler compiler, Sources sources, Variable variable, Map<String, Argument> arguments, Context context) {
        if (variable.methodAllocations.peek().containsKey(name)) {
            compiler.address(variable.methodAllocations.peek().get(name).location());
            return;
        }
        if (variable.allocations.containsKey(name)) {
            compiler.address(variable.allocations.get(name).location());
            return;
        }
        int address = compiler.address();
        variable.methodAllocations.peek().put(name, new Variable.Allocation(4, address));
    }

}
