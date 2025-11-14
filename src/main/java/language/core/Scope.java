package language.core;

import java.util.*;

public class Scope {

    public record Allocation(int size, int location) {}

    public static class Generic {
        public enum Type { Structure, Document }
        public Type type;
        public boolean known;
        public String name;
        public Structure structure;
        public Document document;
        public ArrayList<Generic> generics = new ArrayList<>();
    }

    int operationCount = 0;
    public Scope parent;
    public String name;
    public Structure structure;
    public HashMap<String, Scope> scopes = new HashMap<>();
    public LinkedHashMap<String, Generic> generics = new LinkedHashMap<>();
    public HashMap<String, Allocation> allocations = new HashMap<>();
    public HashMap<String, Integer> literals = new HashMap<>();
    public HashMap<String, Block> blocks = new HashMap<>();
    public HashMap<String, String> names = new HashMap<>();
    public ArrayList<String> defaultArgs = new ArrayList<>();
    public Scope implicitScope;

    public static Scope withImplicit() {
        Scope s = new Scope();
        s.implicitScope = new Scope();
        return s;
    }

    public Scope add(String name) {
        Scope newScope = Scope.withImplicit();
        newScope.parent = this;
        newScope.name = name;
        this.scopes.put(name, newScope);
        return newScope;
    }

    public void addVariable(String name) {
        scopes.put(name, null);
    }

    public void addImplicit(Scope scope) {
        scopes.putAll(scope.scopes);
        literals.putAll(scope.literals);
        blocks.putAll(scope.blocks);
        defaultArgs.addAll(scope.defaultArgs);
    }

    public void removeImplicit(Scope scope) {
        for (String key : scope.scopes.keySet()) {
            scopes.remove(key);
        }
        for (String key : scope.literals.keySet()) {
            literals.remove(key);
        }
        for (String key : scope.blocks.keySet()) {
            blocks.remove(key);
        }
        defaultArgs.removeAll(scope.defaultArgs);
    }

    public Scope findVariable(String name) {
        if (scopes.containsKey(name)) {
            return scopes.get(name);
        }
        return null;
    }

    public Optional<Integer> findLiteral(String name) {
        if (literals.containsKey(name)) {
            return Optional.of(literals.get(name));
        }
        return Optional.empty();
    }

    public Optional<String> findName(String name) {
        if (names.containsKey(name)) {
            return Optional.of(names.get(name));
        }
        return Optional.empty();
    }

    public Optional<Block> findBlock(String name) {
        if (blocks.containsKey(name)) {
            return Optional.of(blocks.get(name));
        }
        return Optional.empty();
    }

    public Allocation findAllocation(String name) {
        return allocations.get(name);
    }

    public Scope state(String name) {
        if (scopes.containsKey(name)) {
            return scopes.get(name);
        } else {
            Scope newScope = Scope.withImplicit();
            newScope.parent = this;
            newScope.name = name;
            scopes.put(name, newScope);
            return newScope;
        }
    }

    public Scope startOperation(String name) {
        Scope newScope = Scope.withImplicit();
        newScope.parent = this;
        String scopeName;
        if (structure != null) {
            scopeName = structure.name() + "." + name + "#" + operationCount++;
        } else {
            scopeName = name + "#" + operationCount++;
        }
        this.scopes.put(scopeName, newScope);
        return newScope;
    }

    public void add(String name, Allocation allocation) {
        allocations.put(name, allocation);
    }

    public void addState(Scope thisVariable) {
        scopes.putAll(thisVariable.scopes);
        generics.putAll(thisVariable.generics);
    }

    public Scope state() {
        return this;
    }

    public void addDefault(String arg) {
        implicitScope.defaultArgs.add(arg);
    }

    public void remove() {
        implicitScope.defaultArgs.removeLast();
    }

    public List<String> defaults() {
        return defaultArgs;
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
