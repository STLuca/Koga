package language.core;

import core.Document;

import java.util.*;

public class Context {

    public record Allocation(int size, int location) {}

    public static class Generic {
        public enum Type { Structure, Document }
        public Type type;
        public Structure structure;
        public Document document;
    }

    public static class Scope {

        public enum Type {
            Variable,
            Operation
        }

        public Scope parent;
        public Type type;
        public String name;
        public Structure structure;
        public HashMap<String, Scope> scopes = new HashMap<>();
        public LinkedHashMap<String, Generic> generics = new LinkedHashMap<>();
        public HashMap<String, Allocation> allocations = new HashMap<>();
    }

    Scope curr = new Scope();
    ArrayList<Argument> defaultArgs = new ArrayList<>();
    HashMap<String, Argument> implicits = new HashMap<>();

    public Scope add(String name) {
        Scope newScope = new Scope();
        newScope.type = Scope.Type.Variable;
        newScope.parent = curr;
        newScope.name = name;
        curr.scopes.put(name, newScope);
        curr = newScope;
        return curr;
    }

    public void addVariable(String name) {
        curr.scopes.put(name, null);
    }

    public Scope findVariable(String name) {
        Scope curr = this.curr;
        while (curr != null) {
            if (curr.scopes.containsKey(name)) {
                return curr.scopes.get(name);
            }
            curr = curr.parent;
        }
        return null;
    }

    public Scope state(String name) {
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
        return curr;
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

    public Allocation findAllocation(String name) {
        return curr.allocations.get(name);
    }

    public void add(String name, Allocation allocation) {
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
