package language.union;

import language.core.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class Structure implements Usable {

    String name;
    ArrayList<Field> fields = new ArrayList<>();
    ArrayList<Method> constructors = new ArrayList<>();
    ArrayList<Method> methods = new ArrayList<>();

    public int size(Sources sources) {
        int size = 0;
        for (Field f : fields) {
            Usable u = sources.usable(f.usable);
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
    public void declare(Compiler.MethodCompiler compiler, Sources sources, Map<String, Variable> variables, String name, List<String> generics) {

    }

    @Override
    public void construct(Compiler.MethodCompiler compiler, Sources sources, Map<String, Variable> variables, String name, List<String> generics, String constructorName, List<Argument> arguments, Context context) {

    }

    @Override
    public void invoke(Compiler.MethodCompiler compiler, Sources sources, Map<String, Variable> variables, Variable variable, String methodName, List<Argument> arguments, Context context) {
        sources = variable.sources;
        Method method = null;
        for (Method m : methods) {
            if (m.name.equals(methodName)) {
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
            stmt.handle(compiler, sources, variables, argsByName, variable.name + "." + name, context);
        }
    }
}
