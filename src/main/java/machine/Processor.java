package machine;

import core.Types;

public class Processor {

    public static class Snapshot {
        public int instance;
        public int pageMap;

        public boolean interrupted;
        public int interruptValue;
        public int interruptedBy;

        public int object;
        public int table;
        public int instruction;
        public int task;

        public int altObject;
        public int altTable;
        public int altInstruction;
        public int altTask;
    }

    VirtualMachine machine;

    int instance;
    int pageMap;

    boolean interrupted;
    int interruptValue;
    int interruptedBy;

    int task;
    int instruction;
    int altTask;
    int altInstruction;

    int object;
    int table;
    int altObject;
    int altTable;

    public Processor(VirtualMachine machine) {
        this.machine = machine;
    }

    int getRegisterValue(int register) {
        return switch (register) {
            case 0 -> task;
            case 1 -> instruction;
            case 2 -> altTask;
            case 3 -> altInstruction;
            case 4 -> object;
            case 5 -> table;
            case 6 -> altObject;
            case 7 -> altTable;
            default -> throw new RuntimeException();
        };
    }

    public void tick() {
        if (interrupted) {
            machine.interruptable.notify(interruptedBy, interruptValue);
            interrupted = false;
            return;
        }

        byte typeIndex = machine.loadByte(pageMap, instruction);
        byte subType   = machine.loadByte(pageMap, instruction + 1);
        byte inputType = machine.loadByte(pageMap, instruction + 2);
        byte src1Size  = machine.loadByte(pageMap, instruction + 3);
        int  src1In    = machine.loadInt (pageMap, instruction + 4);
        byte src2Size  = machine.loadByte(pageMap, instruction + 8);
        int  src2In    = machine.loadInt (pageMap, instruction + 9);
        byte src3Size  = machine.loadByte(pageMap, instruction + 13);
        int  src3In    = machine.loadInt (pageMap, instruction + 14);
        int instructionSize = 18;

        int startInstruction = instruction;

        Types.Instruction type = Types.Instruction.values()[typeIndex];
        Types.InputType inType = Types.InputType.values()[inputType];

        int src1 = src1In;
        int src2 = 0;
        int src3 = 0;

        switch (inType) {
            case I -> {
                src2 = src2In;
            }
            case T -> {
                src2 = machine.loadInt(pageMap, task + src2In, src2Size);
            }
            case LI -> {
                src2 = getRegisterValue(src2In);
                src3 = src3In;
            }
            case II -> {
                src2 = src2In;
                src3 = src3In;
            }
            case TI -> {
                src2 = machine.loadInt(pageMap, task + src2In, src2Size);
                src3 = src3In;
            }
            case TT -> {
                src2 = machine.loadInt(pageMap, task + src2In, src2Size);
                src3 = machine.loadInt(pageMap, task + src3In, src3Size);
            }
            case III -> {
                src1 = src1In;
                src2 = src2In;
                src3 = src3In;
            }
            case TII -> {
                src1 = machine.loadInt(pageMap, task + src1In, src1Size);
                src2 = src2In;
                src3 = src3In;
            }
            case ITI -> {
                src1 = src1In;
                src2 = machine.loadInt(pageMap, task + src2In, src2Size);
                src3 = src3In;
            }
            case TTT -> {
                src1 = machine.loadInt(pageMap, task + src1In, src1Size);
                src2 = machine.loadInt(pageMap, task + src2In, src2Size);
                src3 = machine.loadInt(pageMap, task + src3In, src3Size);
            }
        }

        switch (type) {
            case Integer -> {
                int result = switch (Types.IntegerType.values()[subType]) {
                    case ADD  -> src2 + src3;
                    case SUB  -> src2 - src3;
                    case SLL  -> src2 << src3;
                    case SEQ  -> src2 == src3 ? 1 : 0;
                    case SNEQ  -> src2 != src3 ? 1 : 0;
                    case SLT  -> src2 < src3 ? 1 : 0;
                    case SLTU -> Integer.compareUnsigned(src2, src3) < 0 ? 1 : 0;
                    case SGT  -> src2 > src3 ? 1 : 0;
                    case SRL  -> src2 >>> src3;
                    case SRA  -> src2 >> src3;
                    case OR   -> src2 | src3;
                    case AND  -> src2 & src3;
                    case XOR  -> src2 ^ src3;
                };
                machine.store(pageMap, task + src1In, result, src1Size);
            }

            case Math -> {
                int result = switch (Types.MathType.values()[subType]) {
                    case MULT  -> src2 * src3;
                };
                machine.store(pageMap, task + src1In, result, src1Size);
            }

            case Jump -> {
                switch (Types.BranchType.values()[subType]) {
                    case REL -> {
                        int destAddr = switch (inType) {
                            case T -> src2 * instructionSize;
                            case I -> src2;
                            default -> throw new RuntimeException("Doesn't support input type: " + inType);
                        };
                        instruction = instruction + destAddr;
                    }
                }
            }


            case ConditionalBranch -> {
                boolean branch = switch (Types.ConditionalBranchType.values()[subType]) {
                    case EQ   -> src2 == src3;
                    case NEQ  -> src2 != src3;
                    case LT   -> src2 < src3;
                    case GTE  -> src2 >= src3;
                    case LTU  -> Integer.compareUnsigned(src2, src3) < 0;
                    case GTEU -> Integer.compareUnsigned(src2, src3) >= 0;
                    default   -> throw new RuntimeException("unsupported");
                };
                if (branch) instruction += src1In;
            }


            case Class -> {
                int offset = switch (Types.ClassType.values()[subType]) {
                    case SIZE -> 0;
                    case ADDR -> 4;
                };
                // src1Val is table, src2Val is symbol
                int size = machine.loadInt(pageMap, src2 + offset + (src3 * 8));
                machine.store(pageMap, task + src1In, size);
            }


            case Logician -> {
                switch (Types.LogicianType.values()[subType]) {
                    case SET_OBJECT     -> object    = src2;
                    case SET_TABLE      -> table     = src2;
                    case SET_ALT_TASK   -> altTask   = src2;
                    case SET_ALT_OBJECT -> altObject = src2;
                    case SET_ALT_TABLE  -> altTable  = src2;

                    case SET_METHOD_AND_TASK -> {
                        instruction = src2;
                        task = src3;
                    }

                    case START_ADMIN -> {
                        int currentTask = task;
                        int currentInstruction = instruction;

                        task = altTask;
                        instruction = src2;

                        altTask = currentTask;
                        altInstruction = currentInstruction + instructionSize;
                    }
                    case STOP_ADMIN -> {
                        int currentTask = task;

                        task = altTask;
                        instruction = altInstruction;

                        altTask = currentTask;
                    }

                    case NOTIFY_SUPERVISOR -> {
                        interrupted = true;
                        interruptedBy = instance;
                        interruptValue = src2;
                    }
                }
            }


            case Memory -> {
                switch (Types.MemoryType.values()[subType]) {
                    case COPY -> {
                        switch (inType) {
                            case III -> {
                                src1 += task;
                                src2 += task;
                            }
                            case TII -> {
                                src2 += task;
                            }
                            case ITI -> {
                                src1 += task;
                            }
                        }
                        for (int i = 0; i < src3; i++) {
                            byte toCopy = machine.loadByte(pageMap, src2 + i);
                            machine.store(pageMap, src1 + i, toCopy);
                        }
                    }
                    case COMPARE -> {

                    }
                }
            }


            case Debug -> {
                int addr = machine.loadInt(pageMap, task + src2In);
                int size = machine.loadInt(pageMap, task + src3In);
                machine.logs.add("instance " + instance + " allocated at " + addr + " of size " + size);
            }
        }

        if (instruction == startInstruction) {
            instruction += 18;
        }

    }

    public void snapshot(Snapshot s) {
        s.instance = instance;
        s.pageMap = pageMap;

        s.interrupted = interrupted;
        s.interruptValue = interruptValue;
        s.interruptedBy = interruptedBy;

        s.object = object;
        s.table = table;
        s.instruction = instruction;
        s.task = task;

        s.altObject = altObject;
        s.altTable = altTable;
        s.altInstruction = altInstruction;
        s.altTask = altTask;
    }

    public void load(Snapshot s) {
        this.instance = s.instance;
        this.pageMap = s.pageMap;

        this.interrupted = s.interrupted;
        this.interruptValue = s.interruptValue;
        this.interruptedBy = s.interruptedBy;

        this.object = s.object;
        this.table = s.table;
        this.instruction = s.instruction;
        this.task = s.task;

        this.altObject = s.altObject;
        this.altTable = s.altTable;
        this.altInstruction = s.altInstruction;
        this.altTask = s.altTask;
    }
}
