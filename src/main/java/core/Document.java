package core;

public class Document {

    public int version = 1;
    public String name;
    public Type type;

    // Host and Hosted
    public int size;
    public String[] dependencies;
    public Const[] consts;
    public Symbol[] symbols;
    public Data[] data;
    public Method[] methods;

    // Host
    public String[] supporting;
    public String administrator;

    // Hosted
    public String[] implementing;

    // Interface
    // public InterfaceMethod[] interfaceMethods;

    // Protocol
    public ProtocolMethod[] protocolMethods;

    // Enums and structs
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
            SYSTEM,

            PROTOCOLS,
            PROTOCOL
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
        public Instruction[] instructions;
        public Data[] data;
        public String[] parameters;
    }

    public record Port(
            int size,
            int permission
    ) {}

    public static class ProtocolMethod {
        public String name;
        public Port[] ports;
    }

}
