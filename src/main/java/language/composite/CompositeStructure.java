package language.composite;

import core.Document;
import language.core.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CompositeStructure implements Structure {

    String name;
    ArrayList<String> imports = new ArrayList<>();
    ArrayList<String> dependencies = new ArrayList<>();
    ArrayList<Generic> generics = new ArrayList<>();
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
    
    public void declare(Compiler.MethodCompiler compiler, Sources sources, Context context, String name, List<String> generics) {
        Variable thisVariable = new Variable();
        thisVariable.name = name;
        thisVariable.structure = this;
        context.add(thisVariable);
        context.state(name);
        context.add(thisVariable);

        for (int i = 0; i < this.generics.size(); i++) {
            Generic generic = this.generics.get(i);
            Context.Generic g = new Context.Generic();
            switch (generic.type) {
                case Structure -> {
                    Structure value = sources.structure(generics.get(i));
                    g.type = Context.Generic.Type.Structure;
                    g.structure = value;
                }
                case Document -> {
                    Document doc = sources.document(generics.get(i), Compilable.Level.Head);
                    g.type = Context.Generic.Type.Document;
                    g.document = doc;
                }
            }
            thisVariable.generics.put(generic.name, g);
        }

        for (String imprt : this.imports) {
            sources.parse(imprt);
        }

        for (String dependency : dependencies) {
            sources.parse(dependency);
            Document document = sources.document(dependency, Compilable.Level.Head);
        }

        for (Field f : fields) {
            Structure u;
            if (thisVariable.generics.containsKey(f.structure)) {
                u = thisVariable.generics.get(f.structure).structure;
            } else {
                u = sources.structure(f.structure);
            }
            String fieldName = f.name;
            u.declare(compiler, sources, context, fieldName, f.generics);
        }
        context.parentState();
    }

    public void proxy(Sources sources, Variable variable, int location) {

    }
    
    public void construct(Compiler.MethodCompiler compiler, Sources sources, Context context, String name, List<String> generics, String constructorName, List<Argument> arguments) {
        Variable thisVariable = new Variable();
        thisVariable.name = name;
        thisVariable.structure = this;
        context.add(thisVariable);
        context.state(name);
        context.add(thisVariable);

        for (int i = 0; i < this.generics.size(); i++) {
            Generic generic = this.generics.get(i);
            Context.Generic g = new Context.Generic();
            switch (generic.type) {
                case Structure -> {
                    Structure value = sources.structure(generics.get(i));
                    g.type = Context.Generic.Type.Structure;
                    g.structure = value;
                }
                case Document -> {
                    Document doc = sources.document(generics.get(i), Compilable.Level.Head);
                    g.type = Context.Generic.Type.Document;
                    g.document = doc;
                }
            }
            thisVariable.generics.put(generic.name, g);
        }

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
            stmt.handle(compiler, sources, argsByName, thisVariable.generics, name, context);
        }
        context.parentState();
    }
    
    public void operate(Compiler.MethodCompiler compiler, Sources sources, Context context, Variable variable, String operationName, List<Argument> arguments) {
        context.state(variable.name);
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
            stmt.handle(compiler, sources, argsByName, variable.generics, variable.name, context);
        }
        context.parentState();
    }
}
