package language.machine;

import core.Document;
import language.core.*;
import language.core.Compiler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MachineUsable implements Usable {

    String name;
    ArrayList<Generic> generics = new ArrayList<>();
    ArrayList<Data> variables = new ArrayList<>();
    ArrayList<String> addresses = new ArrayList<>();
    ArrayList<Method> constructors = new ArrayList<>();
    ArrayList<Method> methods = new ArrayList<>();

    public String name() {
        return name;
    }

    @Override
    public int genericIndex(String name) {
        int i = 0;
        for (Generic g : generics) {
            if (g.name.equals(name)) {
                return i;
            }
            i++;
        }
        return -1;
    }

    public int size(Sources sources) {
        int total = 0;
        for (Data v : variables) {
            total+=v.size;
        }
        return total;
    }

    public void declare(Compiler.MethodCompiler compiler, Sources sources, Map<String, Variable> variables, String name, List<String> generics) {
        if (this.generics.size() != generics.size()) throw new RuntimeException();
        // Setup variable
        Variable variable = new Variable();
        variable.name = name;
        variable.usable = this;
        variables.put(name, variable);
        for (int i = 0; i < this.generics.size(); i++) {
            Generic generic = this.generics.get(i);
            switch (generic.type) {
                case Usable -> {
                    Usable value = sources.usable(generics.get(i));
                    Variable.Generic g = new Variable.Generic();
                    g.type = Variable.Generic.Type.Usable;
                    g.usable = value;
                    variable.generics.add(g);
                }
                case Document -> {
                    Document doc = sources.document(generics.get(i), Compilable.Level.Head);
                    Variable.Generic g = new Variable.Generic();
                    g.type = Variable.Generic.Type.Document;
                    g.document = doc;
                    variable.generics.add(g);
                }
            }
        }
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
    }
    
    public void proxy(Sources sources, Variable variable, int location) {
        // Setup variable
        variable.usable = this;
        // generics

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

    public void construct(Compiler.MethodCompiler compiler, Sources sources, Map<String, Variable> variables, String name, List<String> generics, String constructorName, List<Argument> args, Context context) {
        Variable variable = new Variable();
        variable.name = name;
        variable.usable = this;
        variables.put(name, variable);
        // Setup variable
        if (this.generics.size() != generics.size()) throw new RuntimeException();
        for (int i = 0; i < this.generics.size(); i++) {
            Generic generic = this.generics.get(i);
            switch (generic.type) {
                case Usable -> {
                    Usable value = sources.usable(generics.get(i));
                    Variable.Generic g = new Variable.Generic();
                    g.type = Variable.Generic.Type.Usable;
                    g.usable = value;
                    variable.generics.add(g);
                }
                case Document -> {
                    Document doc = sources.document(generics.get(i), Compilable.Level.Head);
                    Variable.Generic g = new Variable.Generic();
                    g.type = Variable.Generic.Type.Document;
                    g.document = doc;
                    variable.generics.add(g);
                }
            }
        }

        // Try and match a constructor
        Method c = null;
        for (Method con : constructors) {
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
        for (Method.Parameter p : c.parameters) {
            argsByName.put(p.name, args.get(i));
            i++;
        }

        for (String address : addresses) {
            int addr = compiler.address();
            variable.allocations.put(address, new Variable.Allocation(4, addr));
        }
        for (Data v : this.variables) {
            int location = compiler.data(v.size);
            variable.allocations.put(v.name, new Variable.Allocation(v.size, location));
            compiler.debugData(variable.name, v.name, location, v.size);
        }

        variable.methodAllocations.push(new HashMap<>());
        for (Statement s : c.body) {
            s.compile(compiler, sources, variable, argsByName, context);
        }
        variable.methodAllocations.pop();
    }

    public void invoke(Compiler.MethodCompiler compiler, Sources sources, Map<String, Variable> variables, Variable variable, String methodName, List<Argument> args, Context context) {
        // Find the method
        Method method = null;
        for (Method m : methods) {
            if (m.matches(variable, methodName, args)) {
                method = m;
                break;
            }
        }
        if (method == null) {
            throw new RuntimeException(String.format("Can't match method %s", methodName));
        }

        // Map the args to name using parameters
        HashMap<String, Argument> argsByName = new HashMap<>();
        int i = 0;
        for (Method.Parameter param : method.parameters) {
            argsByName.put(param.name, args.get(i++));
        }

        variable.methodAllocations.add(new HashMap<>());
        for (Statement s : method.body) {
            s.compile(compiler, sources, variable, argsByName, context);
        }
        variable.methodAllocations.pop();
    }

}
