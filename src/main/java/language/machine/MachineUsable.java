package language.machine;

import language.core.*;
import language.core.Compiler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MachineUsable implements Usable {

    String name;
    List<String> generics = new ArrayList<>();
    List<Data> variables = new ArrayList<>();
    List<String> addresses = new ArrayList<>();
    List<Constructor> constructors = new ArrayList<>();
    List<Method> methods = new ArrayList<>();

    
    public String name() {
        return name;
    }

    public int size() {
        int total = 0;
        for (Data v : variables) {
            total+=v.size;
        }
        return total;
    }

    public void declare(Compiler.MethodCompiler compiler, Classes classes, Variable variable, List<String> generics) {
        // Setup variable
        variable.clazz = this;
        if (this.generics.size() != generics.size()) throw new RuntimeException();
        for (int i = 0; i < generics.size(); i++) {
            Usable value = classes.usable(generics.get(i));
            if (value == null) break;
            variable.generics.put(this.generics.get(i), value);
        }
        compiler.pushContext();
        // setup data and addresses
        for (String address : addresses) {
            int addr = compiler.address();
            variable.allocations.put(address, new Variable.Allocation(4, addr));
        }
        for (Data v : this.variables) {
            if (v.size > 0) {
                int location = compiler.data(v.size);
                variable.allocations.put(v.name, new Variable.Allocation(v.size, location));
                compiler.debugData(variable.name, v.name, location, v.size);
            }
        }
        compiler.popContext();
    }

    
    public void proxy(Classes classes, Variable variable, int location) {
        // Setup variable
        variable.clazz = this;
        if (this.generics.size() != generics.size()) throw new RuntimeException();
        for (int i = 0; i < generics.size(); i++) {
            Usable value = classes.usable(generics.get(i));
            if (value == null) break;
            variable.generics.put(this.generics.get(i), value);
        }
        // setup data and addresses
//        for (String address : addresses) {
//            int addr = compiler.address();
//            variable.allocations.put(address, new Variable.Allocation(4, addr));
//        }
        for (Data v : this.variables) {
            if (v.size > 0) {
                variable.allocations.put(v.name, new Variable.Allocation(v.size, location));
                location += v.size;
            }
        }
    }

    public void construct(Compiler.MethodCompiler compiler, Classes classes, Map<String, Variable> variables, Variable variable, List<String> generics, String constructorName, List<Argument> args) {
        // Setup variable
        variable.clazz = this;
        if (this.generics.size() != generics.size()) throw new RuntimeException();
        for (int i = 0; i < generics.size(); i++) {
            variable.generics.put(this.generics.get(i), classes.usable(generics.get(i)));
        }

        // Try and match a constructor
        Constructor c = null;
        for (Constructor con : constructors) {
            if (con.matches(constructorName, args)) {
                c = con;
                break;
            }
        }
        if (c == null) throw new RuntimeException("No constructor found");

        // Map the args to their name using generics and parameters
        Map<String, Argument> argsByName = new HashMap<>();
        int i = 0;
        for (Method.Parameter p : c.parameters) {
            argsByName.put(p.name, args.get(i));
            i++;
        }

        Variable admin = variables.get("admin");
        if (admin != null) {
            argsByName.put("admin", Argument.of(admin));
        }

        compiler.pushContext();
        for (String address : addresses) {
            int addr = compiler.address();
            variable.allocations.put(address, new Variable.Allocation(4, addr));
        }
        for (Data v : this.variables) {
            if (v.size > 0) {
                int location = compiler.data(v.size);
                variable.allocations.put(v.name, new Variable.Allocation(v.size, location));
                compiler.debugData(variable.name, v.name, location, v.size);
            }
        }

        variable.methodAllocations.push(new HashMap<>());
        for (Statement s : c.body) {
            s.compile(compiler, variable, admin, argsByName);
        }
        compiler.popContext();
        variable.methodAllocations.pop();
    }

    public void invoke(Compiler.MethodCompiler compiler, Map<String, Variable> variables, Variable variable, String methodName, List<Argument> args) {
        // Find the method (only by name atm)
        Method method = methods.stream().filter(m -> m.matches(methodName, args)).findFirst().orElseThrow();

        // Map the args to name using parameters
        Map<String, Argument> argsByName = new HashMap<>();
        int i = 0;
        for (Method.Parameter param : method.parameters) {
            argsByName.put(param.name, args.get(i++));
        }

        Variable admin = variables.get("admin");
        if (admin != null) {
            argsByName.put("admin", Argument.of(admin));
        }

        compiler.pushContext();
        variable.methodAllocations.add(new HashMap<>());
        for (Statement s : method.body) {
            s.compile(compiler, variable, admin, argsByName);
        }
        compiler.popContext();
        variable.methodAllocations.pop();
    }

}
