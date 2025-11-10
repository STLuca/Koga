package language.core;

import core.Document;

import java.util.*;

public class Scope {

    public enum Type {
        Variable,
        Operation
    }

    public record Allocation(int size, int location) {}

    public static class Generic {
        public enum Type { Structure, Document }
        public Type type;
        public Structure structure;
        public Document document;
    }

    public Scope parent;
    public Type type;
    public String name;
    public Structure structure;
    public HashMap<String, Scope> scopes = new HashMap<>();
    public LinkedHashMap<String, Generic> generics = new LinkedHashMap<>();
    public HashMap<String, Allocation> allocations = new HashMap<>();
    public HashMap<String, Scope> implicit = new HashMap<>();

    static ArrayList<Argument> defaultArgs = new ArrayList<>();
    static HashMap<String, Argument> implicits = new HashMap<>();

    public static Scope reset() {
        defaultArgs = new ArrayList<>();
        implicits = new HashMap<>();
        return new Scope();
    }

    public Scope add(String name) {
        Scope newScope = new Scope();
        newScope.type = Scope.Type.Variable;
        newScope.parent = this;
        newScope.name = name;
        this.scopes.put(name, newScope);
        return newScope;
    }

    public void addVariable(String name) {
        scopes.put(name, null);
    }

    public Scope findVariable(String name) {
        Scope curr = this;
        while (curr != null) {
            if (curr.scopes.containsKey(name)) {
                return curr.scopes.get(name);
            }
            curr = curr.parent;
        }
        return null;
    }

    public Scope state(String name) {
        if (scopes.containsKey(name)) {
            return scopes.get(name);
        } else {
            Scope newScope = new Scope();
            newScope.type = Scope.Type.Variable;
            newScope.parent = this;
            newScope.name = name;
            scopes.put(name, newScope);
            return newScope;
        }
    }

    public Scope parentState() {
        return parent;
    }

    public Scope startOperation() {
        Scope newScope = new Scope();
        newScope.type = Scope.Type.Operation;
        newScope.parent = this;
        this.scopes.put(UUID.randomUUID().toString(), newScope);
        return newScope;
    }

    public Scope stopOperation() {
        return parent;
    }

    public Allocation findAllocation(String name) {
        return allocations.get(name);
    }

    public void add(String name, Allocation allocation) {
        allocations.put(name, allocation);
    }

    public Scope state() {
        return this;
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
        Scope curr = this;
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
