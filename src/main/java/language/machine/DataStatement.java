package language.machine;

import language.core.*;

import java.util.ArrayList;
import java.util.Map;

public class DataStatement implements Statement {

    String name;
    ArrayList<String> sizes = new ArrayList<>();
    
    public void compile(Compiler.MethodCompiler compiler, Sources sources, Variable variable, Map<String, Argument> arguments, Context context) {
        int allocateSize = 1;
        for (int i = 0; i < sizes.size(); i+= 2) {
            InputType inputType = InputType.valueOf(sizes.get(i));
            int size = inputType.resolve(sizes.get(i + 1), variable, arguments, context).value();
            allocateSize *= size;
        }
        int allocated = compiler.data(allocateSize);
        if (variable.allocations.containsKey(name)) {
            variable.allocations.put(name, new Variable.Allocation(allocateSize, allocated));
        } else {
            context.add(name, new Variable.Allocation(allocateSize, allocated));
        }
        compiler.debugData(variable.name, name, allocated, allocateSize);
    }

}
