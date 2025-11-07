package language.core;

import java.util.*;

public class Context {

    int operation = -1;

    HashMap<String, Variable> variables = new HashMap<>();
    ArrayList<Argument> defaultArgs = new ArrayList<>();
    HashMap<String, Argument> implicits = new HashMap<>();
    ArrayList<Map<String, Variable.Allocation>> operations = new ArrayList<>();

    public void add(Variable variable) {
        variables.put(variable.name, variable);
    }

    public Variable variable(String name) {
        return variables.get(name);
    }


    public void startOperation() {
        operations.addLast(new HashMap<>());
        operation++;
    }

    public void stopOperation() {
        operations.removeLast();
        operation--;
    }

    public Variable.Allocation operationAllocation(String name) {
        return operations.get(operation).get(name);
    }

    public void operationAllocation(String name, Variable.Allocation allocation) {
        operations.get(operation).put(name, allocation);
    }


    public int state() {
        return operation;
    }

    public void setState(int newState) {
        operation = newState;
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
