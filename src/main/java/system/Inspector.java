package system;

import machine.VirtualMachine;

import java.util.*;

// Can this just have a reference to Administrator instead of calculating everything again?
// This will have methods used to introspect, read variable x for object o
public class Inspector {

    static class Task {
        Map<String, Map<String, Integer>> data = new HashMap<>();
        Map<String, Map<String, Integer>> altData = new HashMap<>();
        int task;
        int object;
        int instruction;
        int table;
        int altTask;
        int altObject;
        int altInstruction;
        int altTable;
    }
    static class Thread {
        List<Task> tasks = new ArrayList<>();
    }
    static class RuntimeClass {
        String runtimeTable;
        Map<String, String> methods = new HashMap<>();
    }
    static class RuntimeObject {
        RuntimeClass clazz;
        Map<String, Integer> data = new HashMap<>();
    }

    int pageMap;
    VirtualMachine machine;
    RuntimeClass runtimeClasses;
    List<Task> tasks = new ArrayList<>();
    List<Task> processorTasks = new ArrayList<>();
    Map<Integer, byte[]> objects = new HashMap<>();

    int load(int address) {
        return machine.loadInt(pageMap, address);
    }

    void load(int address, byte[] bytes) {
        machine.load(pageMap, address, bytes);
    }

}
