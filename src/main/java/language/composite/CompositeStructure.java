package language.composite;

import language.core.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CompositeStructure implements Structure {

    String name;
    ArrayList<String> imports = new ArrayList<>();
    ArrayList<String> dependencies = new ArrayList<>();
    ArrayList<Field> fields = new ArrayList<>();
    ArrayList<Method> constructors = new ArrayList<>();
    ArrayList<Method> methods = new ArrayList<>();
    
    public String name() {
        return name;
    }
    
    public int size(Sources sources) {
        int size = 0;
        for (Field f : fields) {
            Structure u = sources.structure(f.name);
            size += u.size(sources);
        }
        return size;
    }
    
    public void declare(Compiler.MethodCompiler compiler, Sources sources, Map<String, Variable> variables, String name, List<String> generics) {
        Variable thisVariable = new Variable();
        thisVariable.name = name;
        thisVariable.structure = this;
        variables.put(name, thisVariable);

        for (String imprt : this.imports) {
            sources.parse(imprt);
        }

        for (Field f : fields) {
            Structure u = sources.structure(f.structure);
            String fieldName = name + "." + f.name;
            u.declare(compiler, sources, variables, fieldName, f.generics);
        }
    }

    public void proxy(Sources sources, Variable variable, int location) {

    }
    
    public void construct(Compiler.MethodCompiler compiler, Sources sources, Map<String, Variable> variables, String name, List<String> generics, String constructorName, List<Argument> arguments, Context context) {
        Variable thisVariable = new Variable();
        thisVariable.name = name;
        thisVariable.structure = this;
        variables.put(name, thisVariable);

        for (String imprt : this.imports) {
            sources.parse(imprt);
        }

        Method method = null;
        for (Method m : constructors) {
            if (m.name.equals(constructorName)) {
                method = m;
                break;
            }
        }
        if (method == null) throw new RuntimeException("Method not found");

        // Map the args to name using parameters
        HashMap<String, language.core.Argument> argsByName = new HashMap<>();
        int i = 0;
        for (Parameter param : method.params) {
            argsByName.put(param.name, arguments.get(i++));
        }

        for (Statement stmt : method.statements) {
            stmt.handle(compiler, sources, variables, argsByName, name, context);
        }
    }
    
    public void operate(Compiler.MethodCompiler compiler, Sources sources, Map<String, Variable> variables, Variable variable, String operationName, List<Argument> arguments, Context context) {
        Method method = null;
        for (Method m : methods) {
            if (m.name.equals(operationName)) {
                method = m;
                break;
            }
        }
        if (method == null) throw new RuntimeException("Method not found");

        // Map the args to name using parameters
        HashMap<String, language.core.Argument> argsByName = new HashMap<>();
        int i = 0;
        for (Parameter param : method.params) {
            argsByName.put(param.name, arguments.get(i++));
        }

        for (Statement stmt : method.statements) {
            stmt.handle(compiler, sources, variables, argsByName, variable.name, context);
        }

    }
}
