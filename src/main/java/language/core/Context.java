package language.core;

import java.util.*;

public class Context {

    public static class Scope {

        enum Type {
            Variable,
            Operation
        }

        Scope parent;
        Type type;
        String name;
        Structure structure;
        HashMap<String, Variable> variables = new HashMap<>();
        HashMap<String, Scope> scopes = new HashMap<>();
        LinkedHashMap<String, Variable.Generic> generics = new LinkedHashMap<>();
        HashMap<String, Variable.Allocation> allocations = new HashMap<>();
    }

    Scope curr = new Scope();
    ArrayList<Argument> defaultArgs = new ArrayList<>();
    HashMap<String, Argument> implicits = new HashMap<>();

    public void add(Variable variable) {
        if (curr.parent != null && curr.parent.variables.containsKey(variable.name)) {
            curr.parent.variables.put(variable.name, variable);
        } else {
            curr.variables.put(variable.name, variable);
        }
    }

    public void addVariable(String name) {
        curr.variables.put(name, null);
    }

    public Variable findVariable(String name) {
        Scope curr = this.curr;
        while (curr != null) {
            if (curr.variables.containsKey(name)) {
                return curr.variables.get(name);
            }
            curr = curr.parent;
        }
        return null;
    }


    public void state(String name) {
        if (curr.scopes.containsKey(name)) {
            curr = curr.scopes.get(name);
        } else {
            Scope newScope = new Scope();
            newScope.type = Scope.Type.Variable;
            newScope.parent = curr;
            newScope.name = name;
            curr.scopes.put(name, newScope);
            curr = newScope;
        }
    }

    public void parentState() {
        curr = curr.parent;
    }


    public void startOperation() {
        Scope newScope = new Scope();
        newScope.type = Scope.Type.Operation;
        newScope.parent = curr;
        curr.scopes.put(UUID.randomUUID().toString(), newScope);
        curr = newScope;
    }

    public void stopOperation() {
        curr = curr.parent;
    }

    public Variable.Allocation findAllocation(String name) {
        return curr.allocations.get(name);
    }

    public void add(String name, Variable.Allocation allocation) {
        curr.allocations.put(name, allocation);
    }


    public Scope state() {
        return curr;
    }

    public void setState(Scope s) {
        curr = s;
    }


    public void add(Argument arg) {
        defaultArgs.add(arg);
    }

    public void remove() {
        defaultArgs.removeLast();
    }

    public List<Argument> defaults() {
        return defaultArgs;
    }

    public void add(String name, Argument arg) {
        implicits.put(name, arg);
    }

    public Optional<Argument> get(String name) {
        return Optional.ofNullable(implicits.get(name));
    }

    public void remove(String name) {
        implicits.remove(name);
    }

    public String stateName(String name) {
        StringBuilder sb = new StringBuilder();
        Scope curr = this.curr;
        while (curr.parent != null) {
            if (curr.name != null) {
                sb.insert(0, curr.name)
                        .insert(curr.name.length(), ".");
            }
            curr = curr.parent;
        }
        sb.append(name);
        return sb.toString();

    }


}
