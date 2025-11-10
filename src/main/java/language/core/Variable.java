package language.core;

import java.util.*;

public class Variable {

    public String name;
    public Structure structure;
    public LinkedHashMap<String, Context.Generic> generics = new LinkedHashMap<>();
    public HashMap<String, Context.Allocation> allocations = new HashMap<>();

}
