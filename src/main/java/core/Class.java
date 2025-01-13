package core;

import java.util.ArrayList;
import java.util.List;

public class Class {

    public int version = 1;
    public String name;
    public Type type;
    public int size;
    public List<String> implementing = new ArrayList<>();
    public List<Const> consts = new ArrayList<>();
    public List<Symbol> symbols = new ArrayList<>();
    public List<Data> data = new ArrayList<>();
    public List<Method> methods = new ArrayList<>();

    public enum Type {
        Host,      // Host owns the memory
        Hosted,    // Hosted exists in a hosts memory
        Interface, // Should this be a subtype of hosted?
        Protocol   //
    }

    public static class Symbol {

        public Type type;
        public String identifier;

        public enum Type {
            CLASS,
            INTERFACE,
            FIELD,
            METHOD,
            CONST,
            SYSTEM
        }
    }

    public static class Const {

        public String name;
        public byte[] value;

    }

    public record Data(
            String name,
            int start,
            int size
    ) {}

    public static class Method {
        public String name;
        public int size;
        public List<Instruction> instructions = new ArrayList<>();
        public List<Data> data = new ArrayList<>();
    }

}
