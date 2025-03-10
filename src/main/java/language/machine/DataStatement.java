package language.machine;

import language.core.Argument;
import language.core.Compiler;
import language.core.Context;
import language.core.Variable;

import java.util.Map;

public class DataStatement implements Statement {

    String name;
    int size;

    
    public void compile(Compiler.MethodCompiler compiler, Variable variable, Map<String, Argument> arguments, Context context) {
        int allocated = compiler.data(size);
        variable.methodAllocations.peek().put(name, new Variable.Allocation(size, allocated));
        compiler.debugData(variable.name, name, allocated, size);
    }

}
