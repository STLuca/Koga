package language.core;

import java.util.*;

public class Scope {

    public record Allocation(int size, int location) {}

    public static class Generic {
        public enum Type { Structure, Document }
        public Scope.Generic.Type type;
        public boolean known;
        public String name;
        public Structure structure;
        public Document document;
        public ArrayList<Scope.Generic> generics = new ArrayList<>();
    }

    Scope parent;
    String name;
    Structure structure;
    HashMap<String, Scope> namedSubScopes = new HashMap<>();
    ArrayList<Scope> unnamedSubScopes = new ArrayList<>();
    Scope currentAnonymous;
    LinkedHashMap<String, Scope.Generic> generics = new LinkedHashMap<>();
    HashMap<String, Scope.Allocation> allocations = new HashMap<>();
    HashMap<String, Integer> literals = new HashMap<>();
    HashMap<String, Block> blocks = new HashMap<>();
    HashMap<String, String> names = new HashMap<>();
    ArrayList<String> defaultArgs = new ArrayList<>();
    Scope implicitScope;

    public static Scope withImplicit() {
        Scope s = new Scope();
        s.implicitScope = new Scope();
        return s;
    }

    public Scope state(Structure structure, String name) {
        if (name.equals("_")) {
            Scope newScope = Scope.withImplicit();
            newScope.parent = this;
            newScope.name = name;
            newScope.structure = structure;
            unnamedSubScopes.add(newScope);
            currentAnonymous = newScope;
            return newScope;
        }
        if (namedSubScopes.containsKey(name)) {
            return namedSubScopes.get(name);
        } else {
            Scope newScope = Scope.withImplicit();
            newScope.parent = this;
            newScope.name = name;
            newScope.structure = structure;
            namedSubScopes.put(name, newScope);
            return newScope;
        }
    }

    public Scope startOperation(String name) {
        Scope newScope = Scope.withImplicit();
        newScope.name = name;
        newScope.parent = this;
        this.unnamedSubScopes.add(newScope);
        return newScope;
    }


    public Scope parent() {
        return parent;
    }


    public String name() {
        return name;
    }

    public String stateName(String name) {
        StringBuilder sb = new StringBuilder();
        Scope curr = this;
        while (curr.parent != null) {
            if (curr.structure != null) {
                sb.insert(0, curr.name)
                        .insert(curr.name.length(), ".");
            }
            curr = curr.parent;
        }
        sb.append(name);
        return sb.toString();
    }


    public Structure structure() {
        return structure;
    }


    public void put(String name, Scope scope) {
        namedSubScopes.put(name, scope);
    }

    public void removeVariable(String name) {
        namedSubScopes.remove(name);
    }

    public Optional<Scope> findVariable(String name) {
        if (name.equals("_")) {
            return Optional.ofNullable(currentAnonymous);
        }
        return Optional.ofNullable(namedSubScopes.get(name));
    }


    public void put(String name, Scope.Generic generic) {
        generics.put(name, generic);
    }

    public Optional<Scope.Generic> findGeneric(String name) {
        return Optional.ofNullable(generics.get(name));
    }

    public int genericSize() {
        return generics.size();
    }

    public List<Scope.Generic> generics() {
        return generics.sequencedValues().stream().toList();
    }


    public void put(String name, Scope.Allocation allocation) {
        allocations.put(name, allocation);
    }

    public Optional<Scope.Allocation> findAllocation(String name) {
        return Optional.ofNullable(allocations.get(name));
    }

    public Iterable<Scope.Allocation> allocations() {
        return allocations.values();
    }


    public void put(String name, Integer literal) {
        literals.put(name, literal);
    }

    public Optional<Integer> findLiteral(String name) {
        return Optional.ofNullable(literals.get(name));
    }


    public void put(String name, Block block) {
        blocks.put(name, block);
    }

    public Optional<Block> findBlock(String name) {
        return Optional.ofNullable(blocks.get(name));
    }


    public void put(String name, String value) {
        names.put(name, value);
    }

    public Optional<String> findName(String name) {
        return Optional.ofNullable(names.get(name));
    }


    public Scope implicit() {
        return implicitScope;
    }

    public void addImplicit(Scope scope) {
        namedSubScopes.putAll(scope.namedSubScopes);
        literals.putAll(scope.literals);
        blocks.putAll(scope.blocks);
        defaultArgs.addAll(scope.defaultArgs);
    }

    public void removeImplicit(Scope scope) {
        for (String key : scope.namedSubScopes.keySet()) {
            namedSubScopes.remove(key);
        }
        for (String key : scope.literals.keySet()) {
            literals.remove(key);
        }
        for (String key : scope.blocks.keySet()) {
            blocks.remove(key);
        }
        defaultArgs.removeAll(scope.defaultArgs);
    }

    public void add(String name, Scope.Allocation allocation) {
        allocations.put(name, allocation);
    }

    public void addState(Scope thisVariable) {
        namedSubScopes.putAll(thisVariable.namedSubScopes);
        generics.putAll(thisVariable.generics);
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

}
