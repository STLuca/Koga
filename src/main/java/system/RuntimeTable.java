package system;

import machine.VirtualMachine;

import java.util.ArrayList;
import java.util.List;

// Rename RuntimeTableBuilder
public class RuntimeTable {

    ArrayList<Integer> table = new ArrayList<>();
    StringBuilder debug = new StringBuilder();

    void document(String docName) {
        debug.append("Table: ")
                .append(docName)
                .append("\n");
    }

    void intrface(String concreteName, String interfaceName) {
        debug.append("Table: ")
                .append(concreteName)
                .append(".")
                .append(interfaceName)
                .append("\n");
    }

    void document(String docName, int size, int addr) {
        debug(docName, size, addr);
    }

    void method(String docName, String m, int size, int addr) {
        debug(docName + "." + m, size, addr);
    }

    void field(String docName, String field, int size, int addr) {
        debug(docName + "." + field, size, addr);
    }

    void intrface(String concreteName, String interfaceName, int addr) {
        debug(concreteName + "." + interfaceName, 0, addr);
    }

    void constant(String name, int size, int addr) {
        debug("Const(" + name + ")", size, addr);
    }

    void protocolMethod(String name, int id, int addr) {
        debug("Protocol method(" + name + ")", id, addr, "id", "N/A");
    }

    void empty() {
        debug("BAD SYMBOL - FIX ", 0, 0);
    }

    void system(String type, int addr) {
        debug("System(" + type + ")", VirtualMachine.PAGE_SIZE, addr * VirtualMachine.PAGE_SIZE, "pageSize", "addr");
    }

    void debug(String name, int size, int addr) {
        debug(name, size, addr, "size", "addr");
    }

    void debug(String name, int size, int addr, String primary, String secondary) {
        int byteIndx = table.size() * 4;
        pad(byteIndx);
        debug.append(byteIndx)
                .append(": ");
        pad(size);
        debug.append(size)
                .append("    ")
                .append(name)
                .append(".")
                .append(primary)
                .append("\n");
        pad(byteIndx + 4);
        debug.append(byteIndx + 4)
                .append(": ");
        pad(addr);
        debug.append(addr)
                .append("    ")
                .append(name)
                .append(".")
                .append(secondary)
                .append("\n");
        table.add(size);
        table.add(addr);
    }

    void pad(int val) {
        if (val < 10) debug.append(" ");
        if (val < 100) debug.append(" ");
        if (val < 1000) debug.append(" ");
    }
}
