package core;

public class Instruction {

    public Type type;

    public IntegerType lType;
    public BranchType jType;
    public ConditionalBranchType bType;
    public ClassType cmType;
    public MemoryType mType;
    public LogicianType lgType;
    public InterruptType iType;
    public DebugType dType;
    public MathType mathType;
    public FloatType fType;
    public AtomicType aType;
    public VectorType vType;

    public InputType inputType;

    public int src1Size;
    public int src1;
    public int src2Size;
    public int src2;
    public int src3Size;
    public int src3;

    public enum Type {
        Integer,
        Jump,
        ConditionalBranch,
        Class,
        Memory,
        Logician,
        Math,
        Float,
        Atomic,
        Vector,
        Debug,
    }

    /*
        I - Immediate
        Addresses:
        L - Logician
        T - Task
        O - Object
        H - Host
     */
    public enum InputType {
        NONE,
        I,
        T,

        II,
        LI,
        TI,
        TT,

        III,
        TII,
        ITI,
        TTT
    }

    public enum IntegerType {
        ADD,
        SUB,
        SLL,
        SEQ,
        SNEQ,
        SLT,
        SLTU,
        SGT,
        SRL,
        SRA,
        OR,
        AND,
        XOR
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
        COMPARE
    }

    public enum LogicianType {
        SET,
        SET_OBJECT,
        SET_TABLE,
        SET_ALT_OBJECT,
        SET_ALT_TABLE,
        SET_ALT_TASK,

        SET_METHOD_AND_TASK,

        START_ADMIN,
        STOP_ADMIN,

        STORE,
        RESTORE,

        NOTIFY_SUPERVISOR,
    }

    public enum DebugType {
        ALLOCATED
    }

    public enum InterruptType {
        PORT_WRITTEN
    }

    public enum MathType {
        MULT
    }

    public enum FloatType {
        TODO
    }

    public enum AtomicType {
        TODO
    }

    public enum VectorType {
        TODO
    }
}
