package language.core;

import java.util.*;

public interface Scope {

    record Allocation(int size, int location) {}

    interface Block {
        void execute(Compiler.MethodCompiler compiler, Scope scope);
    }

    class Description {
        public enum Type { Structure, Document }
        public Description.Type type;
        public Structure structure;
        public Document document;
        public ArrayList<Description> generics = new ArrayList<>();

        public boolean equals(Description generic) {
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

    Scope state(Description description, String name);
    Scope startOperation(Scope state, String name);

    Description description();

    void put(String name, Scope scope);
    Optional<Scope> findVariable(String name);

    void put(String name, Description generic);
    Optional<Description> findGeneric(String name);

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
