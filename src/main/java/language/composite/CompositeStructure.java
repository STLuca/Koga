package language.composite;

import core.Document;
import language.core.*;

import java.util.ArrayList;
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
    
    public void declare(Compiler.MethodCompiler compiler, Sources sources, Scope scope, String name, List<String> generics, List<GenericArgument> nestedGenerics) {
        Scope thisVariable = scope.state(name);
        thisVariable.name = name;
        thisVariable.structure = this;

        for (int i = 0; i < this.generics.size(); i++) {
            Generic generic = this.generics.get(i);
            Scope.Generic g = new Scope.Generic();
            g.name = generic.name;
            g.known = true;
            switch (generic.type) {
                case Structure -> {
                    Structure value = sources.structure(generics.get(i));
                    g.type = Scope.Generic.Type.Structure;
                    g.structure = value;
                }
                case Document -> {
                    Document doc = sources.document(generics.get(i), Compilable.Level.Head);
                    g.type = Scope.Generic.Type.Document;
                    g.document = doc;
                }
            }
            thisVariable.generics.put(generic.name, g);
        }

        for (String imprt : this.imports) {
            sources.structure(imprt);
        }

        for (String dependency : dependencies) {
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
            u.declare(compiler, sources, thisVariable, fieldName, f.generics, null);
        }
    }

    public void proxy(Sources sources, Scope variable, int location) {

    }
    
    public void construct(Compiler.MethodCompiler compiler, Sources sources, Scope scope, String name, List<String> generics, List<GenericArgument> nestedGenerics, String constructorName, List<String> argumentNames) {
        Scope thisVariable = scope.state(name);
        thisVariable.name = name;
        thisVariable.structure = this;

        for (int i = 0; i < this.generics.size(); i++) {
            Generic generic = this.generics.get(i);
            Scope.Generic g = new Scope.Generic();
            g.name = generic.name;
            g.known = true;
            switch (generic.type) {
                case Structure -> {
                    Structure value = sources.structure(generics.get(i));
                    g.type = Scope.Generic.Type.Structure;
                    g.structure = value;
                }
                case Document -> {
                    Document doc = sources.document(generics.get(i), Compilable.Level.Head);
                    g.type = Scope.Generic.Type.Document;
                    g.document = doc;
                }
            }
            thisVariable.generics.put(generic.name, g);
        }

        for (String imprt : this.imports) {
            sources.structure(imprt);
        }

        Method method = null;
        for (Method m : constructors) {
            if (m.name.equals(constructorName)) {
                method = m;
                break;
            }
        }
        if (method == null) throw new RuntimeException("Method not found");

        Scope operationScope = thisVariable.startOperation(constructorName);
        // Map the args to name using parameters
        int i = 0;
        for (Parameter param : method.params) {
            String argName = argumentNames.get(i++);
            switch (param.type) {
                case Variable -> {
                    Scope v = scope.findVariable(argName);
                    if (v == null) {
                        throw new RuntimeException();
                    }
                    operationScope.scopes.put(param.name, v);
                }
                case Block -> {
                    Block b = scope.findBlock(argName).orElseThrow();
                    operationScope.blocks.put(param.name, b);
                }
            }

        }

        for (Statement stmt : method.statements) {
            stmt.handle(compiler, sources, name, operationScope);
        }
    }
    
    public void operate(Compiler.MethodCompiler compiler, Sources sources, Scope scope, Scope variable, String operationName, List<String> arguments) {
        Method method = null;
        for (Method m : methods) {
            if (m.name.equals(operationName)) {
                method = m;
                break;
            }
        }
        if (method == null) throw new RuntimeException("Method not found");

        Scope operationScope = variable.startOperation(operationName);
        operationScope.scopes.putAll(variable.scopes);
        // Map the args to name using parameters
        int i = 0;
        for (Parameter param : method.params) {
            String arg = arguments.get(i++);
            switch (param.type) {
                case Variable -> {
                    Scope v = scope.findVariable(arg);
                    operationScope.scopes.put(param.name, v);
                }
                case Block -> {
                    Block b = scope.findBlock(arg).orElseThrow();
                    operationScope.blocks.put(param.name, b);
                }
                case null, default -> {

                }
            }
        }
        for (Statement stmt : method.statements) {
            stmt.handle(compiler, sources, variable.name, operationScope);
        }
    }
}
