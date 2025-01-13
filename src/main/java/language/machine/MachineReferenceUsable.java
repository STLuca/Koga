package language.machine;

import language.core.*;
import language.core.Compiler;

import java.util.*;

public class MachineReferenceUsable implements Usable {

    String name;
    List<Data> variables = new ArrayList<>();
    List<String> addresses = new ArrayList<>();
    List<Constructor> constructors = new ArrayList<>();
    String referencedClass; // the generic name
    Method invokeMethod;
    Statement argStatement;
    Method argMethod;

    
    public String name() {
        return name;
    }

    
    public void declare(Compiler.MethodCompiler compiler, Classes classes, Variable variable, List<String> generics) {
        if (referencedClass != null){
            if (generics.size() != 1) throw new RuntimeException();
            variable.compilableGenerics.put(referencedClass, classes.compilable(generics.get(0)));
        }
    }

    
    public void proxy(Classes classes, Variable variable, int location) {
        throw new RuntimeException("Not supported");
    }

    public void construct(Compiler.MethodCompiler compiler, Classes classes, Map<String, Variable> variables, Variable variable, List<String> generics, String constructorName, List<Argument> args) {
        if (referencedClass != null){
            if (generics.size() != 1) throw new RuntimeException();
            variable.compilableGenerics.put(referencedClass, classes.compilable(generics.get(0)));
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

        // Map the args to their name using referencedClass and parameters
        Map<String, Argument> argsByName = new HashMap<>();

        // setup parameter arguments
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
            if (v.size == 0) continue;
            int location = compiler.data(v.size);
            variable.allocations.put(v.name, new Variable.Allocation(v.size, location));
            compiler.debugData(variable.name, v.name, location, v.size);
        }

        variable.methodAllocations.push(new HashMap<>());
        for (Statement s : c.body) {
            s.compile(compiler, variable, admin, argsByName);
        }
        compiler.popContext();
        variable.methodAllocations.pop();
    }

    public void invoke(Compiler.MethodCompiler compiler, Map<String, Variable> variables, Variable variable, String methodName, List<Argument> args) {
        Map<String, Argument> argsByName = new HashMap<>();

        // put the name instead of the arguments
        argsByName.put("methodName", Argument.of(methodName));

        Variable admin = variables.get("admin");
        if (admin != null) {
            argsByName.put("admin", Argument.of(admin));
        }

        compiler.pushContext();
        variable.methodAllocations.push(new HashMap<>());

        for (Statement s : invokeMethod.body) {
            if (argStatement != s) {
                s.compile(compiler, variable, admin, argsByName);
                continue;
            }
            // For each argument, invoke the argMethod
            Map<String, Argument> argArgs = new HashMap<>();
            for (Argument arg : args) {
                compiler.pushContext();
                argArgs.put(argMethod.parameters.get(0).name, arg);
                for (Statement as : argMethod.body) {
                    as.compile(compiler, variable, admin, argArgs);
                }
                compiler.popContext();
            }
        }
        compiler.popContext();
        variable.methodAllocations.pop();
    }

    public int size() {
        int total = 0;
        for (Data v : variables) {
            total+=v.size;
        }
        return total;
    }

}
