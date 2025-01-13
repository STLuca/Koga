package language.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

// Maybe this should be an interface
// CompilerClass has declare and construct, then variable has invoke?
// but why?
public class Variable {
    public record Allocation(int size, int location) {}

    public String name;
    public Usable clazz;
    public Map<String, Usable> generics = new HashMap<>();
    public Map<String, Compilable> compilableGenerics = new HashMap<>();
    public Map<String, Allocation> allocations = new HashMap<>();
    public Stack<Map<String, Allocation>> methodAllocations = new Stack<>();

}
