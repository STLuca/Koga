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
        if (namedSubScopes.containsKey(name)) {
            return namedSubScopes.get(name);
        }

        Scope newScope = Scope.withImplicit();
        newScope.parent = this;
        newScope.name = this.name == null ? name : this.name + "." + name;
        newScope.structure = structure;

        if (name.equals("_")) {
            unnamedSubScopes.add(newScope);
            currentAnonymous = newScope;
        } else {
            namedSubScopes.put(name, newScope);
        }

        return newScope;
    }

    public Scope startOperation(Scope state, String name) {
        Scope newScope = Scope.withImplicit();
        newScope.name = this.name == null ? name : this.name + "." + name;
        newScope.parent = this;
        unnamedSubScopes.add(newScope);

        newScope.namedSubScopes.putAll(state.namedSubScopes);
        newScope.literals.putAll(state.literals);
        newScope.blocks.putAll(state.blocks);
        newScope.defaultArgs.addAll(state.defaultArgs);
        newScope.allocations.putAll(state.allocations);
        newScope.generics.putAll(state.generics);

        newScope.namedSubScopes.putAll(implicitScope.namedSubScopes);
        newScope.literals.putAll(implicitScope.literals);
        newScope.blocks.putAll(implicitScope.blocks);
        newScope.defaultArgs.addAll(implicitScope.defaultArgs);
        newScope.allocations.putAll(implicitScope.allocations);

        newScope.implicitScope.namedSubScopes.putAll(implicitScope.namedSubScopes);
        newScope.implicitScope.literals.putAll(implicitScope.literals);
        newScope.implicitScope.blocks.putAll(implicitScope.blocks);
        newScope.implicitScope.defaultArgs.addAll(implicitScope.defaultArgs);
        newScope.implicitScope.allocations.putAll(implicitScope.allocations);
        return newScope;
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


    public void addDefault(Scope arg) {
        String[] split = arg.name.split("\\.");
        implicitScope.defaultArgs.add(split[split.length - 1]);
    }

    public void removeLastDefault() {
        implicitScope.defaultArgs.removeLast();
    }

    public List<String> defaults() {
        return defaultArgs;
    }

    public void debugData(Compiler.MethodCompiler methodCompiler) {
        HashSet<Scope> handled = new HashSet<>();
        ArrayDeque<Scope> scopes = new ArrayDeque<>();
        scopes.push(this);
        while (!scopes.isEmpty()) {
            Scope s = scopes.pop();
            if (handled.contains(s)) continue;
            handled.add(s);
            for (Map.Entry<String, Allocation> entry : s.allocations.entrySet()) {
                methodCompiler.debugData(s.name + "." + entry.getKey(), entry.getValue().location, entry.getValue().size);
            }
            scopes.addAll(s.namedSubScopes.values());
        }
    }

}
