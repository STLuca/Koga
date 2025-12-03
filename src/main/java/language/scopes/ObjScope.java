package language.scopes;

import language.core.Compiler;
import language.core.Scope;

import java.util.*;

public class ObjScope implements Scope {

    ObjScope parent;
    String name;
    Description description;
    HashMap<String, ObjScope> namedSubScopes = new HashMap<>();
    ArrayList<ObjScope> unnamedSubScopes = new ArrayList<>();
    ObjScope currentAnonymous;
    HashMap<String, Description> generics = new HashMap<>();
    HashMap<String, Allocation> allocations = new HashMap<>();
    HashMap<String, Integer> addresses = new HashMap<>();
    HashMap<String, byte[]> literals = new HashMap<>();
    HashMap<String, Block> blocks = new HashMap<>();
    HashMap<String, String> names = new HashMap<>();
    ArrayList<ObjScope> defaultArgs = new ArrayList<>();
    ObjScope implicitScope;

    public static ObjScope rootOperation(Scope stateScope) {
        ObjScope state = (ObjScope) stateScope;
        ObjScope s = new ObjScope();
        s.implicitScope = new ObjScope();
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

    public static ObjScope rootState() {
        return new ObjScope();
    }

    public ObjScope state(Description description, String name) {
        if (namedSubScopes.containsKey(name)) {
            return namedSubScopes.get(name);
        }
        ObjScope newScope = new ObjScope();
        newScope.parent = this;
        newScope.name = this.name == null ? name : this.name + "." + name;
        newScope.description = description;

        if (name.equals("_")) {
            unnamedSubScopes.add(newScope);
            currentAnonymous = newScope;
        } else {
            namedSubScopes.put(name, newScope);
        }

        return newScope;
    }

    public ObjScope startOperation(Scope stateScope, String name) {
        ObjScope state = (ObjScope) stateScope;
        ObjScope newScope = new ObjScope();
        newScope.implicitScope = new ObjScope();
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


    public Description description() {
        return description;
    }

    public void put(String name, Scope scopeState) {
        ObjScope scope = (ObjScope) scopeState;
        namedSubScopes.put(name, scope);
    }

    public Optional<Scope> findVariable(String name) {
        if (name.equals("_")) {
            return Optional.ofNullable(currentAnonymous);
        }
        return Optional.ofNullable(namedSubScopes.get(name));
    }


    public void put(String name, Description generic) {
        generics.put(name, generic);
    }

    public Optional<Description> findGeneric(String name) {
        return Optional.ofNullable(generics.get(name));
    }


    public void put(String name, Allocation allocation) {
        allocations.put(name, allocation);
    }

    public Optional<Allocation> findAllocation(String name) {
        return Optional.ofNullable(allocations.get(name));
    }

    public Optional<Allocation> allocation() {
        int start = Integer.MAX_VALUE;
        int size = 0;
        for (Allocation a : allocations.values()) {
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


    public void addDefault(Scope argScope) {
        ObjScope arg = (ObjScope) argScope;
        implicitScope.defaultArgs.add(arg);
    }

    public void removeLastDefault() {
        implicitScope.defaultArgs.removeLast();
    }

    public List<Scope> defaults() {
        return Collections.unmodifiableList(defaultArgs);
    }

    public void debugData(Compiler.MethodCompiler methodCompiler) {
        HashSet<ObjScope> handled = new HashSet<>();
        ArrayDeque<ObjScope> scopes = new ArrayDeque<>();
        scopes.push(this);
        while (!scopes.isEmpty()) {
            ObjScope s = scopes.pop();
            if (handled.contains(s)) continue;
            handled.add(s);
            for (Map.Entry<String, Allocation> entry : s.allocations.entrySet()) {
                methodCompiler.debugData(s.name + "." + entry.getKey(), entry.getValue().location(), entry.getValue().size());
            }
            scopes.addAll(s.namedSubScopes.values());
        }
    }

}
