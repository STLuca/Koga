package system;

import core.Class;
import machine.VirtualMachine;

import java.util.ArrayList;
import java.util.List;

// Rename RuntimeTableBuilder
public class RuntimeTable {

    List<Integer> table = new ArrayList<>();
    StringBuilder debug = new StringBuilder();

    void clazz(Class c) {
        debug.append("Table: ")
                .append(c.name)
                .append("\n");
    }

    void intrface(Class c, Class i) {
        debug.append("Table: ")
                .append(c.name)
                .append(".")
                .append(i.name)
                .append("\n");
    }

    void clazz(Class c, int size, int addr) {
        debug(c.name, size, addr);
    }

    void method(Class c, String m, int size, int addr) {
        debug(c.name + "." + m, size, addr);
    }

    void field(Class c, String field, int size, int addr) {
        debug(c.name + "." + field, size, addr);
    }

    void intrface(Class c, Class i, int addr) {
        debug(c.name + "." + i.name, 0, addr);
    }

    void constant(Class c, String name, int size, int addr) {
        debug("Const(" + name + ")", size, addr);
    }

    void empty(Class c) {
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
