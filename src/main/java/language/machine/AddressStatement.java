package language.machine;

import language.core.*;

import java.util.Map;

public class AddressStatement implements Statement {

    String name;
    
    public void compile(Compiler.MethodCompiler compiler, Sources sources, Variable variable, Map<String, Argument> arguments, Context context) {
        if (context.findAllocation(name) != null) {
            compiler.address(context.findAllocation(name).location());
            return;
        }
        if (variable.allocations.containsKey(name)) {
            compiler.address(variable.allocations.get(name).location());
            return;
        }
        int address = compiler.address();
        Context.Allocation allocation = new Context.Allocation(4, address);
        context.add(name, allocation);
    }

}
