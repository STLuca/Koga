package core;

public class Instruction {

    public Type type;

    public LogicType lType;
    public BranchType jType;
    public ConditionalBranchType bType;
    public ClassType cmType;
    public MemoryType mType;
    public LogicianType lgType;
    public InterruptType iType;
    public DebugType dType;

    public InputType inputType;

    public int destSize;
    public int dest;
    public int src1Size;
    public int src1;
    public int src2Size;
    public int src2;

    public enum Type {
        Logic,
        Jump,
        ConditionalBranch,
        Class,
        Memory,
        Logician,
        Interrupt,
        Debug
    }

    /*
        R - Register (TODO: change to L, for logician)
        I - Immediate
        A - Address
        P - Pointer
        Consider naming address location and pointer address?
        Consider not combining
     */
    public enum InputType {
        NONE,
        R,
        I,
        A,
        P,
        II,
        RI,
        AR,
        AI,
        AA,
        AP,
        PA,
        PP,
    }

    public enum LogicType {
        ADD,
        SUB,
        SLL,
        SLT,
        SLTU,
        SGT, // maybe remove and use slt?
        SRL,
        SRA,
        OR,
        AND,
        MUL
    }

    public enum BranchType {
        REL
    }

    public enum ConditionalBranchType {
        EQ,
        NEQ,
        LT,
        GTE,
        LTU,
        GTEU
    }

    public enum ClassType {
        SIZE,
        ADDR
    }

    public enum MemoryType {
        COPY,
        // COMPARE e.g. for String?
    }

    // Replace getters with register source input
    // maybe replace setters too, would mean needed a dest input type
    public enum LogicianType {
        SET_OBJECT,
        SET_TABLE,
        SET_METHOD_AND_TASK,
        SET_ALT_OBJECT,
        SET_ALT_TABLE,
        SET_ALT_TASK,

        START_ADMIN,
        STOP_ADMIN,

        INTERRUPT
    }

    public enum DebugType {
        ALLOCATED
    }

    public enum InterruptType {
        PORT_WRITTEN
    }
}
