package language.core;

import java.util.*;

public interface Scope {

    record Allocation(int size, int location) {}

    interface Block {
        void execute(Compiler.MethodCompiler compiler, Scope scope);
    }

    class Generic {
        public enum Type { Structure, Document }
        public Scope.Generic.Type type;
        public Structure structure;
        public Document document;
        public ArrayList<Scope.Generic> generics = new ArrayList<>();

        public boolean equals(Generic generic) {
            boolean basic =  type == generic.type
                    && Objects.equals(structure, generic.structure)
                    && Objects.equals(document, generic.document)
                    && generics.size() <= generic.generics.size();
            if (!basic) {
                return false;
            }
            for (int i = 0; i < generics.size(); i++) {
                if (!generics.get(i).equals(generic.generics.get(i))) {
                    return false;
                }
            }
            return true;
        }

    }

    Scope state(Generic description, String name);
    Scope startOperation(Scope state, String name);

    Generic description();

    void put(String name, Scope scope);
    Optional<Scope> findVariable(String name);

    void put(String name, Scope.Generic generic);
    Optional<Scope.Generic> findGeneric(String name);

    void put(String name, Scope.Allocation allocation);
    Optional<Scope.Allocation> findAllocation(String name);
    Optional<Scope.Allocation> allocation();

    void putAddress(String name, Integer address);
    Optional<Integer> findAddress(String name);

    void put(String name, int val);
    void put(String name, byte[] val);
    Optional<byte[]> findLiteral(String name);
    Optional<Integer> findLiteralAsInt(String name);

    void put(String name, Block block);
    Optional<Block> findBlock(String name);

    void put(String name, String value);
    Optional<String> findName(String name);

    Scope implicit();

    void addDefault(Scope arg);
    void removeLastDefault();
    List<Scope> defaults();

    void debugData(Compiler.MethodCompiler methodCompiler);

}
