package language.enumeration;

import language.core.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

class Structure implements language.core.Structure {

    String name;
    ArrayList<Field> fields = new ArrayList<>();
    ArrayList<Method> constructors = new ArrayList<>();
    ArrayList<Method> methods = new ArrayList<>();

    public int size(Sources sources) {
        int size = 0;
        for (Field f : fields) {
            language.core.Structure u = sources.structure(f.structure);
            size += u.size(sources);
        }
        return size;
    }

    @Override
    public String name() {
        return "";
    }

    @Override
    public void proxy(Sources sources, Variable variable, int location) {

    }

    @Override
    public void declare(Compiler.MethodCompiler compiler, Sources sources, Context context, String name, List<String> generics) {

    }

    @Override
    public void construct(Compiler.MethodCompiler compiler, Sources sources, Context context, String name, List<String> generics, String constructorName, List<Argument> arguments) {

    }

    @Override
    public void operate(Compiler.MethodCompiler compiler, Sources sources, Context context, Variable variable, String operationName, List<Argument> arguments) {
        Method method = null;
        for (Method m : methods) {
            if (m.name.equals(operationName)) {
                method = m;
                break;
            }
        }
        if (method == null) throw new RuntimeException("Method not found");

        // Map the args to name using parameters
        HashMap<String, Argument> argsByName = new HashMap<>();
        int i = 0;
        for (Parameter param : method.params) {
            argsByName.put(param.name, arguments.get(i++));
        }

        for (Statement stmt : method.statements) {
            stmt.handle(compiler, sources, argsByName, variable.name + "." + name, context);
        }
    }
}
