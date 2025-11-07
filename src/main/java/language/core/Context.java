package language.core;

import java.util.*;

public class Context {

    HashMap<String, Variable> variables = new HashMap<>();
    ArrayList<Argument> defaultArgs = new ArrayList<>();
    HashMap<String, Argument> implicits = new HashMap<>();

    public void add(Variable variable) {
        variables.put(variable.name, variable);
    }

    public Variable variable(String name) {
        return variables.get(name);
    }

    public void add(Argument arg) {
        defaultArgs.add(arg);
    }

    public void remove() {
        defaultArgs.removeLast();
    }

    public List<Argument> defaults() {
        return defaultArgs;
    }

    public void add(String name, Argument arg) {
        implicits.put(name, arg);
    }

    public Optional<Argument> get(String name) {
        return Optional.ofNullable(implicits.get(name));
    }

    public void remove(String name) {
        implicits.remove(name);
    }

}
