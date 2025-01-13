package language.core;

import core.Document;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class Variable {
    public record Allocation(int size, int location) {}

    public String name;
    public Usable usable;
    public HashMap<String, Usable> generics = new HashMap<>();
    public HashMap<String, Document> documents = new HashMap<>();
    public HashMap<String, Allocation> allocations = new HashMap<>();
    public ArrayDeque<Map<String, Allocation>> methodAllocations = new ArrayDeque<>();
    public Sources sources;

}
