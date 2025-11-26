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

    public void declare(Compiler.MethodCompiler compiler, Repository repository, Scope scope, Scope.Generic descriptor, String name) {
        // Setup variable
        Scope variable = scope.state(descriptor, name);

        if (this.generics.size() != descriptor.generics.size()) {
            throw new RuntimeException();
        }
        for (int i = 0; i < this.generics.size(); i++) {
            Generic generic = this.generics.get(i);
            Scope.Generic descriptionGeneric = descriptor.generics.get(i);
            variable.put(generic.name, descriptionGeneric);
        }

        for (String address : addresses) {
            int addr = compiler.address();
            variable.putAddress(address, addr);
        }

        for (Data v : this.variables) {
            if (v.size > 0) {
                int location = compiler.data(v.size);
                variable.put(v.name, new Scope.Allocation(v.size, location));
            }
        }
    }

    public void construct(Compiler.MethodCompiler compiler, Repository repository, Scope scope, Scope.Generic descriptor, String name, String constructorName, List<String> arguments) {
        Scope variable = scope.state(descriptor, name);

        // Setup variable
        if (this.generics.size() != descriptor.generics.size()) {
            throw new RuntimeException();
        }
        for (int i = 0; i < this.generics.size(); i++) {
            Generic generic = this.generics.get(i);
            Scope.Generic descriptionGeneric = descriptor.generics.get(i);
            variable.put(generic.name, descriptionGeneric);
        }

        // Try and match a constructor
        Operation c = null;
        for (Operation con : constructors) {
            if (con.matches(variable, scope, constructorName, arguments, repository)) {
                c = con;
                break;
            }
        }
        if (c == null) {
            throw new RuntimeException(String.format("No constructor found for %s.%s", this.name, constructorName));
        }

        for (String address : addresses) {
            int addr = compiler.address();
            variable.putAddress(address, addr);
        }
        for (Data v : this.variables) {
            int location = compiler.data(v.size);
            variable.put(v.name, new Scope.Allocation(v.size, location));
        }

        Scope operationScope = scope.startOperation(variable, constructorName);
        c.populateScope(compiler, scope, operationScope, arguments);

        for (Statement s : c.body) {
            s.compile(compiler, repository, variable, operationScope);
        }
    }

    public void operate(Compiler.MethodCompiler compiler, Repository repository, Scope scope, Scope variable, String operationName, List<String> arguments) {
        // Find the method
        Operation operation = null;
        for (Operation m : operations) {
            if (m.matches(variable, scope, operationName, arguments, repository)) {
                operation = m;
                break;
            }
        }
        if (operation == null) {
            throw new RuntimeException(String.format("Can't match method %s", operationName));
        }

        Scope operationScope = scope.startOperation(variable, operationName);
        operation.populateScope(compiler, scope, operationScope, arguments);

        for (Statement s : operation.body) {
            s.compile(compiler, repository, variable, operationScope);
        }
    }

}
