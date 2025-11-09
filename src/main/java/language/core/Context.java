package language.core;

import java.util.*;

public class Context {

    static class State {
        HashMap<String, State> state = new HashMap<>();
        HashMap<String, Variable> variables = new HashMap<>();
    }

    static class Operation {
        HashMap<String, Variable.Allocation> allocations = new HashMap<>();
        HashMap<String, Variable> variables = new HashMap<>();
    }

    int currOperation = 0;
    int currState = 0;
    ArrayList<String> currentStateName = new ArrayList<>();

    ArrayList<State> state = new ArrayList<>(List.of(new State()));
    ArrayList<Operation> operations = new ArrayList<>(List.of(new Operation()));
    ArrayList<Argument> defaultArgs = new ArrayList<>();
    HashMap<String, Argument> implicits = new HashMap<>();

    public void add(Variable variable) {
        if (state.get(currState).variables.containsKey(variable.name)) {
            state.get(currState).variables.put(variable.name, variable);
        }
        operations.get(currOperation).variables.put(variable.name, variable);
    }

    public void addVariable(String name) {
        state.get(currState).variables.put(name, null);
    }

    public Variable findVariable(String name) {
        if (state.get(currState).variables.containsKey(name)) {
            return state.get(currState).variables.get(name);
        }
        int operation = currOperation;
        while (operation >= 0) {
            if (operations.get(operation).variables.containsKey(name)) {
                return operations.get(operation).variables.get(name);
            }
            operation--;
        }
        return null;
    }


    public void state(String name) {
        State current = state.get(currState);
        current.state.putIfAbsent(name, new State());
        state.add(current.state.get(name));
        currState++;
        currentStateName.add(name);
    }

    public void parentState() {
        state.removeLast();
        currState--;
        currentStateName.removeLast();
    }


    public void startOperation() {
        operations.addLast(new Operation());
        currOperation++;
    }

    public void stopOperation() {
        operations.removeLast();
        currOperation--;
    }

    public Variable.Allocation findAllocation(String name) {
        return operations.get(currOperation).allocations.get(name);
    }

    public void add(String name, Variable.Allocation allocation) {
        operations.get(currOperation).allocations.put(name, allocation);
    }

    public Variable operationVariable(String name) {
        return operations.get(currOperation).variables.get(name);
    }


    public int state() {
        return currOperation;
    }

    public void setState(int newState) {
        currOperation = newState;
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

    public String stateName(String name) {
        StringBuilder sb = new StringBuilder();
        for (String s : currentStateName) {
            sb.append(s).append(".");
        }
        sb.append(name);
        return sb.toString();
    }

}
