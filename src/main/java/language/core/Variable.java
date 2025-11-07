package language.core;

import core.Document;

import java.util.*;

public class Variable {
    public record Allocation(int size, int location) {}

    public String name;
    public Structure structure;
    public LinkedHashMap<String, Generic> generics = new LinkedHashMap<>();
    public HashMap<String, Allocation> allocations = new HashMap<>();
    public ArrayDeque<Map<String, Allocation>> methodAllocations = new ArrayDeque<>();

    public static class Generic {
        public enum Type { Structure, Document }
        public Type type;
        public Structure structure;
        public Document document;
    }

}
