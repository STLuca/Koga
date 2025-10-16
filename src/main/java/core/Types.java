package core;

public class Types {

    // Enums and structs
    public enum Document {
        Host,      // Host owns the memory
        Hosted,    // Hosted exists in a hosts memory
        Interface, // Should this be a subtype of hosted?
        Protocol   //
    }

    public enum Symbol {
        CLASS,
        INTERFACE,
        FIELD,
        METHOD,
        CONST,
        SYSTEM,

        PROTOCOLS,
        PROTOCOL
    }

    public enum Instruction {
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
