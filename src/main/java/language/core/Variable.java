package language.core;

import core.Document;

import java.util.*;

public class Variable {
    public record Allocation(int size, int location) {}

    public String name;
    public Usable usable;
    public ArrayList<Generic> generics = new ArrayList<>();
    public HashMap<String, Allocation> allocations = new HashMap<>();
    public ArrayDeque<Map<String, Allocation>> methodAllocations = new ArrayDeque<>();
    public Sources sources;

    public static class Generic {
        public enum Type { Usable, Document }
        public Type type;
        public Usable usable;
        public Document document;
    }

}
