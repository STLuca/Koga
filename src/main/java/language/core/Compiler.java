package language.core;

import core.Document;
import core.Instruction;

public interface Compiler {

    Document document();
    void name(String name);
    void type(Document.Type type);
    ProtocolCompiler protocol();

    MethodCompiler method();
    void administrator(String name);
    void dependency(String name);
    int symbol(Document.Symbol.Type type, String name);
    int symbol(Document.Symbol.Type type, String document, String name);
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
        int symbol(Document.Symbol.Type type, String name);
        int symbol(Document.Symbol.Type type, String document, String name);
        int constant(byte[] bytes);
        void parameter(String name);
        InstructionCompiler instruction();
        void debugData(String variableName, String name, int location, int size);
    }

    interface InstructionCompiler {

        void type(Instruction.Type type);

        void subType(Instruction.IntegerType subType);
        void subType(Instruction.BranchType subType);
        void subType(Instruction.ConditionalBranchType subType);
        void subType(Instruction.ClassType subType);
        void subType(Instruction.MemoryType subType);
        void subType(Instruction.LogicianType subType);
        void subType(Instruction.InterruptType subType);
        void subType(Instruction.DebugType subType);
        void subType(Instruction.MathType subType);
        void subType(Instruction.AtomicType subType);
        void subType(Instruction.VectorType subType);

        void inputType(Instruction.InputType inputType);

        void src(int location, int size);
        void src(int address);
    }

    interface HostCompiler {
        Document document();
        MethodCompiler method();
        void administrator(String name);
        int symbol(Document.Symbol.Type type, String name);
        int symbol(Document.Symbol.Type type, String document, String name);
        void implementing(String name);
        void constant(String name, byte[] bytes);
        int data(String name, int size);
    }

    interface HostedCompiler {
        void implementing(String name);
        int symbol(Document.Symbol.Type type, String name);
        int symbol(Document.Symbol.Type type, String document, String name);
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

