package language.core;

import core.Class;
import core.Instruction;

public interface Compiler {

    Class clazz();
    void clazz(String name);
    void type(Class.Type type);

    MethodCompiler method();
    int symbol(Class.Symbol.Type type, String name);
    int symbol(Class.Symbol.Type type, String clazz, String name);
    void implementing(String name);
    void constant(String name, byte[] bytes);
    int data(String name, int size);

    interface MethodCompiler {
        void name(String name);
        void pushContext();
        void popContext();
        int data(int size);
        int address();
        void address(int index);
        void view(String type, int addr);
        int symbol(Class.Symbol.Type type, String name);
        int symbol(Class.Symbol.Type type, String clazz, String name);
        int constant(byte[] bytes);
        InstructionCompiler instruction();
        void debugData(String variableName, String name, int location, int size);
    }

    interface InstructionCompiler {

        void type(Instruction.Type type);

        void subType(Instruction.LogicType subType);
        void subType(Instruction.BranchType subType);
        void subType(Instruction.ConditionalBranchType subType);
        void subType(Instruction.ClassType subType);
        void subType(Instruction.MemoryType subType);
        void subType(Instruction.LogicianType subType);
        void subType(Instruction.InterruptType subType);
        void subType(Instruction.DebugType subType);

        void inputType(Instruction.InputType inputType);

        void dest(int location, int size);
        void dest(int address);
        void src(int location, int size);
        void src(int address);
    }

    interface HostCompiler {
        MethodCompiler method();
        int symbol(Class.Symbol.Type type, String name);
        int symbol(Class.Symbol.Type type, String clazz, String name);
        int constant(String name, byte[] bytes);
        int data(String name, int size);
    }

    interface HostedCompiler {
        void implementing(String name);
        int symbol(Class.Symbol.Type type, String name);
        int symbol(Class.Symbol.Type type, String clazz, String name);
        int data(String name, int size);
        MethodCompiler method();
    }

    interface InterfaceCompiler {
        MethodCompiler method();
        void implementing(String name);
    }

    interface ProtocolCompiler {
        MethodCompiler method();
    }

}

