package language.compiling;

import core.Types;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class DocumentStruct {

    public int version = 1;
    public String name;
    public Types.Document type;

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
    // Currently uses methods.name

    // Protocol
    public ProtocolMethod[] protocolMethods;

    public static class Symbol {

        public Types.Symbol type;
        public String identifier;

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
        public String[] parameters;
        public Data[] data;
        public Instruction[] instructions;
    }

    public record Port(
            int size,
            int permission
    ) {}

    public static class ProtocolMethod {
        public String name;
        public Port[] ports;
    }

    public byte[] bytes() {
        try(ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bos)) {

            out.writeUTF("古河市");
            out.writeInt(version);
            out.writeUTF(name);
            out.writeInt(type.ordinal());

            switch (type) {
                case Hosted -> {
                    out.writeInt(size);
                    out.writeInt(dependencies.length);
                    for (String dependency : dependencies) {
                        out.writeUTF(dependency);
                    }

                    out.writeInt(symbols.length);
                    for (Symbol s : symbols) {
                        out.writeInt(s.type.ordinal());
                        out.writeUTF(s.identifier);
                    }

                    int constTotalBytes = 0;
                    for (Const c : consts) {
                        constTotalBytes += c.value.length;
                    }
                    out.writeInt(consts.length);
                    out.writeInt(constTotalBytes);
                    for (Const c : consts) {
                        out.writeUTF(c.name);
                        out.writeInt(c.value.length);
                        out.write(c.value);
                    }

                    out.writeInt(implementing.length);
                    for (String implementing : implementing) {
                        out.writeUTF(implementing);
                    }

                    out.writeInt(data.length);
                    for (Data d : data) {
                        out.writeUTF(d.name);
                        out.writeInt(d.start);
                        out.writeInt(d.size);
                    }

                    int instructionCount = 0;
                    for (Method m : methods) {
                        instructionCount += m.instructions.length;
                    }
                    out.writeInt(methods.length);
                    out.writeInt(instructionCount);
                    for (Method m : methods) {
                        out.writeUTF(m.name);
                        out.writeInt(m.size);
                        out.writeInt(m.parameters.length);
                        for (String p : m.parameters) {
                            out.writeUTF(p);
                        }
                        out.writeInt(m.data.length);
                        for (Data d : m.data) {
                            out.writeUTF(d.name);
                            out.writeInt(d.start);
                            out.writeInt(d.size);
                        }
                        out.writeInt(m.instructions.length);
                        for (Instruction i : m.instructions) {
                            i.write(out);
                        }
                    }
                }
                case Host -> {
                    out.writeInt(size);

                    out.writeUTF(administrator);

                    out.writeInt(dependencies.length);
                    for (String dependency : dependencies) {
                        out.writeUTF(dependency);
                    }

                    out.writeInt(symbols.length);
                    for (Symbol s : symbols) {
                        out.writeInt(s.type.ordinal());
                        out.writeUTF(s.identifier);
                    }

                    int constTotalBytes = 0;
                    for (Const c : consts) {
                        constTotalBytes += c.value.length;
                    }
                    out.writeInt(consts.length);
                    out.writeInt(constTotalBytes);
                    for (Const c : consts) {
                        out.writeUTF(c.name);
                        out.writeInt(c.value.length);
                        out.write(c.value);
                    }

                    out.writeInt(supporting.length);
                    for (String supporting : supporting) {
                        out.writeUTF(supporting);
                    }

                    out.writeInt(data.length);
                    for (Data d : data) {
                        out.writeUTF(d.name);
                        out.writeInt(d.start);
                        out.writeInt(d.size);
                    }

                    int instructionCount = 0;
                    for (Method m : methods) {
                        instructionCount += m.instructions.length;
                    }
                    out.writeInt(methods.length);
                    out.writeInt(instructionCount);
                    for (Method m : methods) {
                        out.writeUTF(m.name);
                        out.writeInt(m.size);
                        out.writeInt(m.parameters.length);
                        for (String p : m.parameters) {
                            out.writeUTF(p);
                        }
                        out.writeInt(m.data.length);
                        for (Data d : m.data) {
                            out.writeUTF(d.name);
                            out.writeInt(d.start);
                            out.writeInt(d.size);
                        }
                        out.writeInt(m.instructions.length);
                        for (Instruction i : m.instructions) {
                            i.write(out);
                        }
                    }
                }
                case Interface -> {
                    out.writeInt(methods.length);
                    for (Method m : methods) {
                        out.writeUTF(m.name);
                    }
                }
                case Protocol -> {
                    out.writeInt(protocolMethods.length);
                    for (ProtocolMethod pm : protocolMethods) {
                        out.writeUTF(pm.name);
                        out.writeInt(pm.ports.length);
                        for (Port p : pm.ports) {
                            out.writeInt(p.size);
                            out.writeInt(p.permission);
                        }
                    }
                }
            }

            out.flush();
            return bos.toByteArray();
        } catch (IOException e) {
           return new byte[0];
        }
    }

}
