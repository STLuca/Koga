package system;

import core.Document;
import core.Instruction;
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


            HashMap<Entry, core.Document.Method> methodByInstruction = new HashMap<>();
            for (Administrator.HostedValues hv : host.template.hostedValues.values()) {
                for (Document.Method m : hv.document.methods) {
                    Administrator.MethodRuntimeValues mrv = hv.methods.get(m.name);
                    Entry entry = new Entry(hv.methodStartAddr + mrv.addr(), hv.methodStartAddr + mrv.endAddr());
                    methodByInstruction.put(entry, m);
                }
            }

            // TODO: currently only works once method has moved one instruction and admin method has moved one instruction
            Entry entry = methodByInstruction.keySet().stream()
                    .filter(e -> t.instruction > e.start && t.instruction <= e.end)
                    .findFirst()
                    .orElseThrow();
            core.Document.Method method = methodByInstruction.get(entry);
            for (core.Document.Data data : method.data) {
                if (data.name().contains(".") && data.size() <= 4) {
                    String[] split = data.name().split("\\.");
                    t.data.putIfAbsent(split[0], new HashMap<>());
                    t.data.get(split[0]).put(split[1], machine.loadInt(host.pageMap, t.task + data.start(), data.size()));
                }
            }

            entry = methodByInstruction.keySet().stream()
                    .filter(e -> t.altInstruction > e.start && t.altInstruction <= e.end)
                    .findFirst()
                    .orElseThrow();

            method = methodByInstruction.get(entry);
            for (core.Document.Data data : method.data) {
                if (data.name().contains(".") && data.size() <= 4) {
                    String[] split = data.name().split("\\.");
                    t.altData.putIfAbsent(split[0], new HashMap<>());
                    t.altData.get(split[0]).put(split[1], machine.loadInt(host.pageMap, t.altTask + data.start(), data.size()));
                }
            }

            tasks.add(t);
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(host.template.runtimeTable.toString());
        for (Administrator.HostedValues hv : host.template.hostedValues.values()) {
            for (Document.Method m : hv.document.methods) {
                sb.append(hv.document.name).append(".").append(m.name).append("\n");
                sb.append("  Data\n");
                for (Document.Data d : m.data) {
                    // TODO: bring this back?
                    // if (!d.name().contains(".")) { continue; }
                    sb.append("    ")
                        .repeat(" ", 5 - String.valueOf(d.start()).length())
                        .append(d.start())
                        .append(": ")
                        .append(d.name())
                        .append("\n");
                }
                sb.append("  Instructions\n");
                int currAddress = hv.methodStartAddr + hv.methods.get(m.name).addr();
                for (Instruction in : m.instructions) {
                     sb.append("    ")
                             .repeat(" ", 5 - String.valueOf(currAddress).length())
                             .append(currAddress)
                             .append(": ");
                     switch(in.type) {
                         case Integer -> sb.append(in.lType);
                         case Jump -> sb.append(in.jType);
                         case ConditionalBranch -> sb.append(in.bType);
                         case Class -> sb.append(in.cmType);
                         case Logician -> sb.append(in.lgType);
                         case Memory -> sb.append(in.mType);
                         case Debug -> sb.append(in.dType);
                     }
                    sb.append(" ")
                            .append(in.inputType)
                            .append(" dest(")
                            .append(in.src1)
                            .append(":")
                            .append(in.src1Size)
                            .append(") src1(")
                            .append(in.src2)
                            .append(":")
                            .append(in.src2Size)
                            .append(") src2(")
                            .append(in.src3)
                            .append(":")
                            .append(in.src3Size)
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
