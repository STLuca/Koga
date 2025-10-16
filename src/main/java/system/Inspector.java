package system;

import core.Types;
import machine.Processor;
import machine.VirtualMachine;

import java.util.*;

public class Inspector {

    Administrator.Host host;
    int pageMap;
    VirtualMachine machine;
    Administrator admin;
    ArrayList<Task> tasks = new ArrayList<>();

    int load(int address) {
        return machine.loadInt(pageMap, address);
    }

    void load(int address, byte[] bytes) {
        machine.load(pageMap, address, bytes);
    }

    public void snapshot() {
        for (Administrator.LogicianQuota quota : admin.quotas) {
            if (quota.host.address != host.address) continue;
            Processor.Snapshot snapshot = quota.snapshot;
            if (quota == admin.scheduled) {
                machine.processors.getFirst().snapshot(snapshot);
            }
            Inspector.Task t = new Inspector.Task();

            t.task = snapshot.task;
            t.object = snapshot.object;
            t.table = snapshot.table;
            t.instruction = snapshot.instruction;
            t.altTask = snapshot.altTask;
            t.altObject = snapshot.altObject;
            t.altTable = snapshot.altTable;
            t.altInstruction = snapshot.altInstruction;


            HashMap<Entry, Administrator.MethodRuntimeValues> methodsByInstruction = new HashMap<>();
            for (Administrator.HostedValues hv : host.template.hostedValues.values()) {
                for (String m : hv.methods.keySet()) {
                    Administrator.MethodRuntimeValues mrv = hv.methods.get(m);
                    Entry entry = new Entry(hv.methodStartAddr + mrv.addr, hv.methodStartAddr + mrv.endAddr);
                    methodsByInstruction.put(entry, hv.methods.get(m));
                }
            }

            // TODO: currently only works once method has moved one instruction and admin method has moved one instruction
            Entry entry = methodsByInstruction.keySet().stream()
                    .filter(e -> t.instruction > e.start && t.instruction <= e.end)
                    .findFirst()
                    .orElseThrow();
            Administrator.MethodRuntimeValues method = methodsByInstruction.get(entry);
            for (Map.Entry<String, Administrator.DataRuntimeValue> dataEntry : method.data.entrySet()) {
                String name = dataEntry.getKey();
                Administrator.DataRuntimeValue data = dataEntry.getValue();
                if (name.contains(".") && data.size() <= 4) {
                    String[] split = name.split("\\.");
                    t.data.putIfAbsent(split[0], new HashMap<>());
                    t.data.get(split[0]).put(split[1], machine.loadInt(host.pageMap, t.task + data.addr(), data.size()));
                }
            }

            entry = methodsByInstruction.keySet().stream()
                    .filter(e -> t.altInstruction > e.start && t.altInstruction <= e.end)
                    .findFirst()
                    .orElseThrow();

            method = methodsByInstruction.get(entry);
            for (Map.Entry<String, Administrator.DataRuntimeValue> dataEntry : method.data.entrySet()) {
                String name = dataEntry.getKey();
                Administrator.DataRuntimeValue data = dataEntry.getValue();
                if (name.contains(".") && data.size() <= 4) {
                    String[] split = name.split("\\.");
                    t.altData.putIfAbsent(split[0], new HashMap<>());
                    t.altData.get(split[0]).put(split[1], machine.loadInt(host.pageMap, t.altTask + data.addr(), data.size()));
                }
            }

            tasks.add(t);
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(host.template.runtimeTable.toString());
        for (Administrator.HostedValues hv : host.template.hostedValues.values()) {
            for (Administrator.MethodRuntimeValues m : hv.methods.values()) {
                sb.append(hv.name).append(".").append(m.name).append("\n");
                sb.append("  Data\n");
                for (Administrator.DataRuntimeValue d : m.data.values()) {
                    // TODO: bring this back?
                    // if (!d.name().contains(".")) { continue; }
                    sb.append("    ")
                        .repeat(" ", 5 - String.valueOf(d.addr()).length())
                        .append(d.addr())
                        .append(": ")
                        .append(d.name())
                        .append("\n");
                }
                sb.append("  Instructions\n");
                int currAddress = hv.methodStartAddr + hv.methods.get(m.name).addr;
                int endAddr = hv.methodStartAddr + hv.methods.get(m.name).endAddr;


                while (currAddress < endAddr) {
                    byte instructionType =      machine.loadByte(pageMap, currAddress);
                    byte instructionSubType =   machine.loadByte(pageMap, currAddress + 1);
                    byte inputTypeVal =         machine.loadByte(pageMap, currAddress + 2);
                    int src1Size =              machine.loadByte(pageMap, currAddress + 3);
                    int src1 =                  machine.loadInt(pageMap, currAddress + 4);
                    int src2Size =              machine.loadByte(pageMap, currAddress + 8);
                    int src2 =                  machine.loadInt(pageMap, currAddress + 9);
                    int src3Size =              machine.loadByte(pageMap, currAddress + 13);
                    int src3 =                  machine.loadInt(pageMap, currAddress + 14);
                    sb.append("    ")
                             .repeat(" ", 5 - String.valueOf(currAddress).length())
                             .append(currAddress)
                             .append(": ");
                    Types.Instruction type = Types.Instruction.values()[instructionType];
                    sb.append(type)
                             .append(" ");
                    switch(type) {
                        case Integer -> sb.append(Types.IntegerType.values()[instructionSubType]);
                        case Jump -> sb.append(Types.BranchType.values()[instructionSubType]);
                        case ConditionalBranch -> sb.append(Types.ConditionalBranchType.values()[instructionSubType]);
                        case Class -> sb.append(Types.ClassType.values()[instructionSubType]);
                        case Logician -> sb.append(Types.LogicianType.values()[instructionSubType]);
                        case Memory -> sb.append(Types.MemoryType.values()[instructionSubType]);
                        case Debug -> sb.append(Types.DebugType.values()[instructionSubType]);
                    }
                    Types.InputType inputType = Types.InputType.values()[inputTypeVal];
                    sb.append(" ")
                            .append(inputType)
                            .append(" dest(")
                            .append(src1)
                            .append(":")
                            .append(src1Size)
                            .append(") src1(")
                            .append(src2)
                            .append(":")
                            .append(src2Size)
                            .append(") src2(")
                            .append(src3)
                            .append(":")
                            .append(src3Size)
                            .append(")");

                    sb.append("\n");
                    currAddress+=18;
                }
            }
        }

        return sb.toString();
    }

    record Entry(int start, int end) {}
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

}
