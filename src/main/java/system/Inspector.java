package system;

import machine.VirtualMachine;

import java.util.*;

public class Inspector {

    static class Task {
        HashMap<String, Map<String, Integer>> data = new HashMap<>();
        HashMap<String, Map<String, Integer>> altData = new HashMap<>();
        int task;
        int object;
        int instruction;
        int table;
        int altTask;
        int altObject;
        int altInstruction;
        int altTable;
    }
    static class Host {
        String runtimeTable;
        HashMap<String, String> methods = new HashMap<>();
    }

    int pageMap;
    VirtualMachine machine;
    Host host;
    ArrayList<Task> tasks = new ArrayList<>();

    int load(int address) {
        return machine.loadInt(pageMap, address);
    }

    void load(int address, byte[] bytes) {
        machine.load(pageMap, address, bytes);
    }

}
