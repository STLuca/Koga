package language.composite;

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
    
    public int size(Repository repository) {
        int size = 0;
        for (Field f : fields) {
            Structure u = repository.structure(f.name);
            size += u.size(repository);
        }
        return size;
    }
    
    public void declare(Compiler.MethodCompiler compiler, Repository repository, Scope scope, String name, List<GenericArgument> generics) {
        Scope thisVariable = scope.state(name);
        thisVariable.structure(this);

        for (int i = 0; i < this.generics.size(); i++) {
            Generic generic = this.generics.get(i);
            Scope.Generic g = new Scope.Generic();
            g.name = generic.name;
            g.known = true;
            switch (generic.type) {
                case Structure -> {
                    Structure value = repository.structure(generics.get(i).name);
                    g.type = Scope.Generic.Type.Structure;
                    g.structure = value;
                }
                case Document -> {
                    language.core.Document doc = repository.document(generics.get(i).name, Compilable.Level.Head);
                    g.type = Scope.Generic.Type.Document;
                    g.document = doc;
                }
            }
            thisVariable.put(generic.name, g);
        }

        for (String imprt : this.imports) {
            repository.structure(imprt);
        }

        for (Field f : fields) {
            ArrayList<String> oldGenerics = new ArrayList<>();
            for (Structure.GenericArgument g : f.generics) {
                oldGenerics.add(g.name);
            }

            Structure u;
            Scope.Generic generic = thisVariable.findGeneric(f.structure).orElse(null);
            if (generic != null) {
                u = generic.structure;
            } else {
                u = repository.structure(f.structure);
            }
            String fieldName = f.name;
            u.declare(compiler, repository, thisVariable, fieldName, f.generics);
        }
    }

    public void proxy(Repository repository, Scope variable, int location) {

    }
    
    public void construct(Compiler.MethodCompiler compiler, Repository repository, Scope scope, String name, List<GenericArgument> generics, String constructorName, List<String> argumentNames) {
        Scope thisVariable = scope.state(name);
        thisVariable.structure(this);

        for (int i = 0; i < this.generics.size(); i++) {
            Generic generic = this.generics.get(i);
            Scope.Generic g = new Scope.Generic();
            g.name = generic.name;
            g.known = true;
            switch (generic.type) {
                case Structure -> {
                    Structure value = repository.structure(generics.get(i).name);
                    g.type = Scope.Generic.Type.Structure;
                    g.structure = value;
                }
                case Document -> {
                    Document doc = repository.document(generics.get(i).name, Compilable.Level.Head);
                    g.type = Scope.Generic.Type.Document;
                    g.document = doc;
                }
            }
            thisVariable.put(generic.name, g);
        }

        for (String imprt : this.imports) {
            repository.structure(imprt);
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
                    Scope v = scope.findVariable(argName).orElseThrow();
                    operationScope.put(param.name, v);
                }
                case Block -> {
                    Block b = scope.findBlock(argName).orElseThrow();
                    operationScope.put(param.name, b);
                }
            }

        }
        operationScope.addState(thisVariable);

        for (Statement stmt : method.statements) {
            stmt.handle(compiler, repository, name, operationScope);
        }
    }
    
    public void operate(Compiler.MethodCompiler compiler, Repository repository, Scope scope, Scope variable, String operationName, List<String> arguments) {
        Method method = null;
        for (Method m : methods) {
            if (m.name.equals(operationName)) {
                method = m;
                break;
            }
        }
        if (method == null) throw new RuntimeException("Method not found");

        Scope operationScope = variable.startOperation(operationName);
        operationScope.addState(variable);
        // Map the args to name using parameters
        int i = 0;
        for (Parameter param : method.params) {
            String arg = arguments.get(i++);
            switch (param.type) {
                case Variable -> {
                    Scope v = scope.findVariable(arg).orElseThrow();
                    operationScope.put(param.name, v);
                }
                case Block -> {
                    Block b = scope.findBlock(arg).orElseThrow();
                    operationScope.put(param.name, b);
                }
                case null, default -> {

                }
            }
        }
        for (Statement stmt : method.statements) {
            stmt.handle(compiler, repository, variable.name(), operationScope);
        }
    }
}
