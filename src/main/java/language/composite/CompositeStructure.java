package language.composite;

import language.core.*;

import java.util.ArrayDeque;
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
    
    public void declare(Compiler.MethodCompiler compiler, Repository repository, Scope scope, String name, List<GenericArgument> generics, Scope.Generic descriptor) {
        Scope thisVariable = scope.state(this, name);

        for (int i = 0; i < this.generics.size(); i++) {
            Generic generic = this.generics.get(i);
            Scope.Generic g = new Scope.Generic();
            switch (generic.type) {
                case Structure -> {
                    Structure value = repository.structure(generics.get(i).name);
                    g.type = Scope.Generic.Type.Structure;
                    g.structure = value;
                }
                case Document -> {
                    language.core.Document doc = repository.document(generics.get(i).name);
                    g.type = Scope.Generic.Type.Document;
                    g.document = doc;
                }
            }
            thisVariable.put(generic.name, g);
        }

        for (Field f : fields) {
            Structure u;
            Scope.Generic generic = thisVariable.findGeneric(f.descriptor.name).orElse(null);
            if (generic != null) {
                u = generic.structure;
            } else {
                u = repository.structure(f.descriptor.name);
            }
            String fieldName = f.name;

            Scope.Generic rootGeneric = new Scope.Generic();
            ArrayDeque<Scope.Generic> dGenerics = new ArrayDeque<>();
            ArrayDeque<Descriptor> descriptors = new ArrayDeque<>();
            dGenerics.push(rootGeneric);
            descriptors.push(f.descriptor);
            while (!dGenerics.isEmpty()) {
                Scope.Generic g = dGenerics.pop();
                Descriptor d = descriptors.pop();
                switch (d.type) {
                    case Structure -> {
                        g.type = Scope.Generic.Type.Structure;
                        g.structure = repository.structure(d.name);
                    }
                    case Document -> {
                        g.type = Scope.Generic.Type.Document;
                        g.document = repository.document(d.name);
                    }
                    case Generic -> {
                        g.type = Scope.Generic.Type.Structure;
                        g.structure = thisVariable.findGeneric(d.name).orElseThrow().structure;
                    }
                }
                for (Descriptor subDescriptor : d.subDescriptors) {
                    descriptors.push(subDescriptor);
                    Scope.Generic subGeneric = new Scope.Generic();
                    g.generics.add(subGeneric);
                    dGenerics.push(subGeneric);
                }
            }

            List<Structure.GenericArgument> arguments = new ArrayList<>();
            for (Descriptor d : f.descriptor.subDescriptors) {
                Structure.GenericArgument arg = new Structure.GenericArgument();
                arg.type = Structure.GenericArgument.Type.Known;
                arg.name = d.name;
                arguments.add(arg);
            }
            
            u.declare(compiler, repository, thisVariable, fieldName, arguments, rootGeneric);
        }
    }
    
    public void construct(Compiler.MethodCompiler compiler, Repository repository, Scope scope, String name, List<GenericArgument> generics, String constructorName, List<String> argumentNames, Scope.Generic descriptor) {
        Scope thisVariable = scope.state(this, name);

        for (int i = 0; i < this.generics.size(); i++) {
            Generic generic = this.generics.get(i);
            Scope.Generic g = new Scope.Generic();
            switch (generic.type) {
                case Structure -> {
                    Structure value = repository.structure(generics.get(i).name);
                    g.type = Scope.Generic.Type.Structure;
                    g.structure = value;
                }
                case Document -> {
                    Document doc = repository.document(generics.get(i).name);
                    g.type = Scope.Generic.Type.Document;
                    g.document = doc;
                }
            }
            thisVariable.put(generic.name, g);
        }

        for (String imprt : this.imports) {
            repository.structure(imprt);
        }

        for (Field field : fields) {
            Structure structure = repository.structure(field.descriptor.name);
            Scope fieldScope = thisVariable.state(structure, field.name);
            // Add field generics?
        }

        Method method = null;
        for (Method m : constructors) {
            if (m.name.equals(constructorName)) {
                method = m;
                break;
            }
        }
        if (method == null) throw new RuntimeException("Method not found");

        Scope operationScope = scope.startOperation(thisVariable, constructorName);
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
                    Scope.Block b = scope.findBlock(argName).orElseThrow();
                    operationScope.put(param.name, b);
                }
            }

        }

        for (Statement stmt : method.statements) {
            stmt.handle(compiler, repository, operationScope);
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

        Scope operationScope = scope.startOperation(variable, operationName);
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
                    Scope.Block b = scope.findBlock(arg).orElseThrow();
                    operationScope.put(param.name, b);
                }
                case null, default -> {

                }
            }
        }
        for (Statement stmt : method.statements) {
            stmt.handle(compiler, repository, operationScope);
        }
    }
}
