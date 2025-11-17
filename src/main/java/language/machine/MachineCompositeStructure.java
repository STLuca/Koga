package language.machine;

import language.core.*;
import language.core.Compiler;

import java.util.ArrayList;
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

    public int size(Repository repository) {
        int total = 0;
        for (Data v : variables) {
            total+=v.size;
        }
        return total;
    }

    public void declare(Compiler.MethodCompiler compiler, Repository repository, Scope scope, String name, List<GenericArgument> generics) {
        if (this.generics.size() != generics.size()) {
            throw new RuntimeException();
        }
        // Setup variable
        Scope variable = scope.state(this, name);

        for (int i = 0; i < this.generics.size(); i++) {
            Generic generic = this.generics.get(i);
            String genericName = generics.get(i).name;
            Scope.Generic scopeGeneric = scope.findGeneric(genericName).orElse(null);
            if (scopeGeneric != null) {
                variable.put(generic.name, scopeGeneric);
                continue;
            }
            Scope.Generic g = new Scope.Generic();
            g.name = generic.name;
            g.known = true;
            switch (generic.type) {
                case Structure -> {
                    Structure value = repository.structure(genericName);
                    g.type = Scope.Generic.Type.Structure;
                    g.structure = value;
                    variable.put(generic.name, g);
                }
                case Document -> {
                    language.core.Document doc = repository.document(genericName, Compilable.Level.Head);
                    g.type = Scope.Generic.Type.Document;
                    g.document = doc;
                    variable.put(generic.name, g);
                }
            }
        }
        // setup data and addresses
        for (String address : addresses) {
            int addr = compiler.address();
            variable.put(address, new Scope.Allocation(4, addr));
        }
        for (Data v : this.variables) {
            if (v.size > 0) {
                int location = compiler.data(v.size);
                variable.put(v.name, new Scope.Allocation(v.size, location));
                compiler.debugData(variable.stateName(v.name), v.name, location, v.size);
            }
        }
    }
    
    public void proxy(Repository repository, Scope variable, int location) {
        // Setup variable
        // variable.structure(this);
        // generics

        // setup data and addresses
//        for (String address : addresses) {
//            int addr = compiler.address();
//            variable.allocations.put(address, new Variable.Allocation(4, addr));
//        }
        for (Data v : this.variables) {
            if (v.size > 0) {
                variable.put(v.name, new Scope.Allocation(v.size, location));
                location += v.size;
            }
        }
    }

    public void construct(Compiler.MethodCompiler compiler, Repository repository, Scope scope, String name, List<GenericArgument> generics, String constructorName, List<String> arguments) {
        Scope variable = scope.state(this, name);

        // Setup variable
        if (this.generics.size() != generics.size()) {
            throw new RuntimeException();
        }
        for (int i = 0; i < this.generics.size(); i++) {
            Generic generic = this.generics.get(i);
            String genericName = generics.get(i).name;
            Scope.Generic scopeGeneric = scope.findGeneric(genericName).orElse(null);
            if (scopeGeneric != null) {
                variable.put(generic.name, scopeGeneric);
                continue;
            }
            Scope.Generic g = new Scope.Generic();
            g.name = generic.name;
            g.known = true;
            switch (generic.type) {
                case Structure -> {
                    g.type = Scope.Generic.Type.Structure;
                    Structure value = repository.structure(genericName);
                    g.structure = value;
                    variable.put(generic.name, g);
                }
                case Document -> {
                    g.type = Scope.Generic.Type.Document;
                    Document doc = repository.document(genericName, Compilable.Level.Head);
                    g.document = doc;
                    variable.put(generic.name, g);
                }
            }
        }

        // Try and match a constructor
        Operation c = null;
        for (Operation con : constructors) {
            if (con.matches(variable, scope, constructorName, arguments)) {
                c = con;
                break;
            }
        }
        if (c == null) {
            throw new RuntimeException(String.format("No constructor found for %s.%s", this.name, constructorName));
        }

        for (String address : addresses) {
            int addr = compiler.address();
            variable.put(address, new Scope.Allocation(4, addr));
        }
        for (Data v : this.variables) {
            int location = compiler.data(v.size);
            variable.put(v.name, new Scope.Allocation(v.size, location));
            compiler.debugData(variable.stateName(v.name), v.name, location, v.size);
        }

        Scope operationScope = variable.startOperation(constructorName);
        int argIdx = 0;
        for (Operation.Parameter p : c.parameters) {
            String arg = arguments.get(argIdx++);
            switch(p.type) {
                case Literal -> {
                    int literal = scope.findLiteral(arg).orElseThrow();
                    operationScope.put(p.name, literal);
                }
                case Variable -> {
                    Scope v = scope.findVariable(arg).orElseThrow();
                    operationScope.put(p.name, v);
                }
                case Block -> {
                    Block b = scope.findBlock(arg).orElseThrow();
                    operationScope.put(p.name, b);
                }
                case Name -> {
                    operationScope.put(p.name, arg);
                }
            }
        }

        for (Statement s : c.body) {
            s.compile(compiler, repository, variable, operationScope);
        }
    }

    public void operate(Compiler.MethodCompiler compiler, Repository repository, Scope scope, Scope variable, String operationName, List<String> arguments) {
        // Find the method
        Operation operation = null;
        for (Operation m : operations) {
            if (m.matches(variable, scope, operationName, arguments)) {
                operation = m;
                break;
            }
        }
        if (operation == null) {
            throw new RuntimeException(String.format("Can't match method %s", operationName));
        }

        // should this be scoped from scope or variable?
        Scope operationScope = scope.startOperation(operationName);
        int argIdx = 0;
        for (Operation.Parameter p : operation.parameters) {
            String arg = arguments.get(argIdx++);
            switch(p.type) {
                case Literal -> {
                    int literal = scope.findLiteral(arg).orElseThrow();
                    operationScope.put(p.name, literal);
                }
                case Variable -> {
                    Scope v = scope.findVariable(arg).orElseThrow();
                    operationScope.put(p.name, v);
                }
                case Block -> {
                    Block b = scope.findBlock(arg).orElseThrow();
                    operationScope.put(p.name, b);
                }
                case Name -> {
                    operationScope.put(p.name, arg);
                }
            }
        }

        for (Statement s : operation.body) {
            s.compile(compiler, repository, variable, operationScope);
        }
    }

}
