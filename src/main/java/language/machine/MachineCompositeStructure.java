package language.machine;

import core.Document;
import language.core.*;
import language.core.Compiler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MachineCompositeStructure implements Structure {

    String name;
    ArrayList<Generic> generics = new ArrayList<>();
    ArrayList<Data> variables = new ArrayList<>();
    ArrayList<String> addresses = new ArrayList<>();
    ArrayList<Operation> constructors = new ArrayList<>();
    ArrayList<Operation> operations = new ArrayList<>();

    public String name() {
        return name;
    }

    public int size(Sources sources) {
        int total = 0;
        for (Data v : variables) {
            total+=v.size;
        }
        return total;
    }

    public void declare(Compiler.MethodCompiler compiler, Sources sources, Scope scope, String name, List<String> generics, List<GenericArgument> nestedGenerics) {
        if (this.generics.size() != generics.size()) {
            throw new RuntimeException();
        }
        // Setup variable
        Scope variable = scope.add(name);
        variable.name = name;
        variable.structure = this;

        for (int i = 0; i < this.generics.size(); i++) {
            Generic generic = this.generics.get(i);
            String genericName = generics.get(i);
            if (scope.generics.containsKey(genericName)) {
                variable.generics.put(generic.name, scope.generics.get(genericName));
                continue;
            }
            Scope.Generic g = new Scope.Generic();
            g.name = generic.name;
            g.known = true;
            switch (generic.type) {
                case Structure -> {
                    Structure value = sources.structure(generics.get(i));
                    g.type = Scope.Generic.Type.Structure;
                    g.structure = value;
                    variable.generics.put(generic.name, g);
                }
                case Document -> {
                    Document doc = sources.document(generics.get(i), Compilable.Level.Head);
                    g.type = Scope.Generic.Type.Document;
                    g.document = doc;
                    variable.generics.put(generic.name, g);
                }
            }
        }
        // setup data and addresses
        for (String address : addresses) {
            int addr = compiler.address();
            variable.allocations.put(address, new Scope.Allocation(4, addr));
        }
        for (Data v : this.variables) {
            if (v.size > 0) {
                int location = compiler.data(v.size);
                variable.allocations.put(v.name, new Scope.Allocation(v.size, location));
                compiler.debugData(variable.stateName(v.name), v.name, location, v.size);
            }
        }
    }
    
    public void proxy(Sources sources, Scope variable, int location) {
        // Setup variable
        variable.structure = this;
        // generics

        // setup data and addresses
//        for (String address : addresses) {
//            int addr = compiler.address();
//            variable.allocations.put(address, new Variable.Allocation(4, addr));
//        }
        for (Data v : this.variables) {
            if (v.size > 0) {
                variable.allocations.put(v.name, new Scope.Allocation(v.size, location));
                location += v.size;
            }
        }
    }

    public void construct(Compiler.MethodCompiler compiler, Sources sources, Scope scope, String name, List<String> generics, List<GenericArgument> nestedGenerics, String constructorName, List<Argument> args) {
        Scope variable = scope.add(name);
        variable.name = name;
        variable.structure = this;

        // Setup variable
        if (this.generics.size() != generics.size()) {
            throw new RuntimeException();
        }
        for (int i = 0; i < this.generics.size(); i++) {
            Generic generic = this.generics.get(i);
            String genericName = generics.get(i);
            if (scope.generics.containsKey(genericName)) {
                variable.generics.put(generic.name, scope.generics.get(genericName));
                continue;
            }
            Scope.Generic g = new Scope.Generic();
            g.name = generic.name;
            g.known = true;
            switch (generic.type) {
                case Structure -> {
                    g.type = Scope.Generic.Type.Structure;
                    Structure value = sources.structure(genericName);
                    g.structure = value;
                    variable.generics.put(generic.name, g);
                }
                case Document -> {
                    g.type = Scope.Generic.Type.Document;
                    Document doc = sources.document(genericName, Compilable.Level.Head);
                    g.document = doc;
                    variable.generics.put(generic.name, g);
                }
            }
        }

        // Try and match a constructor
        Operation c = null;
        for (Operation con : constructors) {
            if (con.matches(variable, constructorName, args)) {
                c = con;
                break;
            }
        }
        if (c == null) {
            throw new RuntimeException(String.format("No constructor found for %s.%s", this.name, constructorName));
        }

        // Map the args to their name using generics and parameters
        HashMap<String, Argument> argsByName = new HashMap<>();
        int i = 0;
        for (Operation.Parameter p : c.parameters) {
            argsByName.put(p.name, args.get(i));
            i++;
        }

        for (String address : addresses) {
            int addr = compiler.address();
            variable.allocations.put(address, new Scope.Allocation(4, addr));
        }
        for (Data v : this.variables) {
            int location = compiler.data(v.size);
            variable.allocations.put(v.name, new Scope.Allocation(v.size, location));
            compiler.debugData(variable.stateName(v.name), v.name, location, v.size);
        }

        Scope operationScope = variable.startOperation(constructorName);
        for (Statement s : c.body) {
            s.compile(compiler, sources, variable, argsByName, operationScope);
        }
    }

    public void operate(Compiler.MethodCompiler compiler, Sources sources, Scope scope, Scope variable, String operationName, List<Argument> args) {
        // Find the method
        Operation operation = null;
        for (Operation m : operations) {
            if (m.matches(variable, operationName, args)) {
                operation = m;
                break;
            }
        }
        if (operation == null) {
            throw new RuntimeException(String.format("Can't match method %s", operationName));
        }

        // Map the args to name using parameters
        HashMap<String, Argument> argsByName = new HashMap<>();
        int i = 0;
        for (Operation.Parameter param : operation.parameters) {
            argsByName.put(param.name, args.get(i++));
        }

        Scope operationScope = variable.startOperation(operationName);
        for (Statement s : operation.body) {
            s.compile(compiler, sources, variable, argsByName, operationScope);
        }
    }

}
