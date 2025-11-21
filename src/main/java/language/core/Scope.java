package language.core;

import java.util.*;

public class Scope {

    public record Allocation(int size, int location) {}

    public interface Block {
        void execute(Compiler.MethodCompiler compiler, Scope scope);
    }

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
    HashMap<String, Integer> addresses = new HashMap<>();
    HashMap<String, byte[]> literals = new HashMap<>();
    HashMap<String, Block> blocks = new HashMap<>();
    HashMap<String, String> names = new HashMap<>();
    ArrayList<Scope> defaultArgs = new ArrayList<>();
    Scope implicitScope;

    public static Scope rootOperation(Scope state) {
        Scope s = new Scope();
        s.implicitScope = new Scope();
        if (state != null) {
            s.namedSubScopes.putAll(state.namedSubScopes);
            s.literals.putAll(state.literals);
            s.blocks.putAll(state.blocks);
            s.defaultArgs.addAll(state.defaultArgs);
            s.allocations.putAll(state.allocations);
            s.generics.putAll(state.generics);
            s.addresses.putAll(state.addresses);
        }
        return s;
    }

    public static Scope rootState() {
        return new Scope();
    }

    public Scope state(Structure structure, String name) {
        if (namedSubScopes.containsKey(name)) {
            return namedSubScopes.get(name);
        }

        Scope newScope = new Scope();
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
        Scope newScope = new Scope();
        newScope.implicitScope = new Scope();
        newScope.name = this.name == null ? name : this.name + "." + name;
        newScope.parent = this;
        unnamedSubScopes.add(newScope);

        newScope.namedSubScopes.putAll(state.namedSubScopes);
        newScope.literals.putAll(state.literals);
        newScope.blocks.putAll(state.blocks);
        newScope.defaultArgs.addAll(state.defaultArgs);
        newScope.allocations.putAll(state.allocations);
        newScope.generics.putAll(state.generics);
        newScope.addresses.putAll(state.addresses);

        newScope.namedSubScopes.putAll(implicitScope.namedSubScopes);
        newScope.literals.putAll(implicitScope.literals);
        newScope.blocks.putAll(implicitScope.blocks);
        newScope.defaultArgs.addAll(implicitScope.defaultArgs);
        newScope.allocations.putAll(implicitScope.allocations);
        newScope.addresses.putAll(implicitScope.addresses);

        newScope.implicitScope.namedSubScopes.putAll(implicitScope.namedSubScopes);
        newScope.implicitScope.literals.putAll(implicitScope.literals);
        newScope.implicitScope.blocks.putAll(implicitScope.blocks);
        newScope.implicitScope.defaultArgs.addAll(implicitScope.defaultArgs);
        newScope.implicitScope.allocations.putAll(implicitScope.allocations);
        newScope.implicitScope.addresses.putAll(implicitScope.addresses);
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

    public Optional<Scope.Allocation> allocation() {
        int start = Integer.MAX_VALUE;
        int size = 0;
        for (Scope.Allocation a : allocations.values()) {
            if (a.location() < start) start = a.location();
            size += a.size();
        }
        if (start == Integer.MAX_VALUE) {
            return Optional.empty();
        }
        return Optional.of(new Allocation(size, start));
    }


    public void putAddress(String name, Integer address) {
        addresses.put(name, address);
    }

    public Optional<Integer> findAddress(String name) {
        return Optional.ofNullable(addresses.get(name));
    }


    public void put(String name, int val) {
        byte[] bytes = new byte[4];
        for (int i = 0; i <= 3; i++) {
            bytes[i] = (byte) (val >>> (8 * i));
        }
        literals.put(name, bytes);
    }

    public void put(String name, byte[] val) {
        literals.put(name, val);
    }

    public Optional<byte[]> findLiteral(String name) {
        return Optional.ofNullable(literals.get(name));
    }

    public Optional<Integer> findLiteralAsInt(String name) {
        if (!literals.containsKey(name)) {
            return Optional.empty();
        }
        byte[] bytes = literals.get(name);
        int r = 0;
        for (int i = 0; i < bytes.length; i++) {
            int bi = bytes[i] & 0xFF;
            r = r | (bi << (8 * i));
        }
        return Optional.of(r);
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
        implicitScope.defaultArgs.add(arg);
    }

    public void removeLastDefault() {
        implicitScope.defaultArgs.removeLast();
    }

    public List<Scope> defaults() {
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
