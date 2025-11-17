package language.machine;

import language.core.*;

import java.util.ArrayList;

public class DataStatement implements Statement {

    String name;
    ArrayList<String> sizes = new ArrayList<>();
    
    public void compile(Compiler.MethodCompiler compiler, Repository repository, Scope variable, Scope scope) {
        int allocateSize = 1;
        for (int i = 0; i < sizes.size(); i+= 2) {
            InputType inputType = InputType.valueOf(sizes.get(i));
            int size = inputType.resolve(sizes.get(i + 1), variable, scope).value();
            allocateSize *= size;
        }
        int allocated = compiler.data(allocateSize);
        Scope.Allocation variableAllocation = variable.findAllocation(name).orElse(null);
        if (variableAllocation != null) {
            variable.put(name, new Scope.Allocation(allocateSize, allocated));
        } else {
            scope.add(name, new Scope.Allocation(allocateSize, allocated));
        }
        compiler.debugData(scope.stateName(name), name, allocated, allocateSize);
    }

}
