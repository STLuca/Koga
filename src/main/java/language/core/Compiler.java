package language.core;

import core.Document;
import core.Types;

public interface Compiler {

    Document document();
    void name(String name);
    void type(Types.Document type);
    ProtocolCompiler protocol();

    MethodCompiler method();
    void administrator(String name);
    void dependency(String name);
    int symbol(Types.Symbol type, String name);
    int symbol(Types.Symbol type, String document, String name);
    void supporting(String protocol);
    void implementing(String name);
    void constant(String name, byte[] bytes);
    int data(String name, int size);

    interface MethodCompiler {
        enum InsertDirection {
            BEFORE,
            AFTER
        }

        void name(String name);
        int data(int size);
        int address();
        void address(int index);
        int position(int addr);
        void direction(InsertDirection direction);
        int symbol(Types.Symbol type, String name);
        int symbol(Types.Symbol type, String document, String name);
        int constant(byte[] bytes);
        void parameter(String name);
        InstructionCompiler instruction();
        void debugData(String variableName, String name, int location, int size);
    }

    interface InstructionCompiler {

        void type(Types.Instruction type);

        void subType(Types.IntegerType subType);
        void subType(Types.BranchType subType);
        void subType(Types.ConditionalBranchType subType);
        void subType(Types.ClassType subType);
        void subType(Types.MemoryType subType);
        void subType(Types.LogicianType subType);
        void subType(Types.InterruptType subType);
        void subType(Types.DebugType subType);
        void subType(Types.MathType subType);
        void subType(Types.AtomicType subType);
        void subType(Types.VectorType subType);

        void inputType(Types.InputType inputType);

        void src(int location, int size);
        void src(int address);
    }

    interface HostCompiler {
        Document document();
        MethodCompiler method();
        void administrator(String name);
        int symbol(Types.Symbol type, String name);
        int symbol(Types.Symbol type, String document, String name);
        void implementing(String name);
        void constant(String name, byte[] bytes);
        int data(String name, int size);
    }

    interface HostedCompiler {
        void implementing(String name);
        int symbol(Types.Symbol type, String name);
        int symbol(Types.Symbol type, String document, String name);
        int data(String name, int size);
        MethodCompiler method();
    }

    interface InterfaceCompiler {
        MethodCompiler method();
        void implementing(String name);
    }

    interface ProtocolCompiler {
        Document document();
        ProtocolMethodCompiler method();
    }

    interface ProtocolMethodCompiler {
        Document.ProtocolMethod method();
        void name(String name);
        void param(int size, int permission);
    }

}

