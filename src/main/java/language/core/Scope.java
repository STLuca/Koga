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

    int operationCount = 0;
    Scope parent;
    String name;
    Structure structure;
    HashMap<String, Scope> scopes = new HashMap<>();
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
            if (curr.name != null) {
                sb.insert(0, curr.name)
                        .insert(curr.name.length(), ".");
            }
            curr = curr.parent;
        }
        sb.append(name);
        return sb.toString();
    }


    public void structure(Structure structure) {
        this.structure = structure;
    }

    public Structure structure() {
        return structure;
    }


    public Scope add(String name) {
        Scope newScope = Scope.withImplicit();
        newScope.parent = this;
        newScope.name = name;
        this.scopes.put(name, newScope);
        return newScope;
    }

    public void putVariable(String name) {
        scopes.put(name, null);
    }

    public void put(String name, Scope scope) {
        scopes.put(name, scope);
    }

    public void putAll(Scope s) {
        scopes.putAll(s.scopes);
    }

    public void removeVariable(String name) {
        scopes.remove(name);
    }

    public Optional<Scope> findVariable(String name) {
        return Optional.ofNullable(scopes.get(name));
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

    public void add(String name, Scope.Allocation allocation) {
        allocations.put(name, allocation);
    }

    public void addState(Scope thisVariable) {
        scopes.putAll(thisVariable.scopes);
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
