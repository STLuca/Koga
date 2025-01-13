package machine;

public class Processor {

    public static class Snapshot {
        public int instance;
        public int pageMap;

        public boolean interrupted;
        public core.Instruction.InterruptType interruptType;
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

    public int instance;
    public int pageMap;

    boolean interrupted;
    core.Instruction.InterruptType interruptType;
    int interruptValue;
    int interruptedBy;

    public int task;
    public int instruction;
    public int altTask;
    public int altInstruction;

    public int object;
    public int table;
    public int altObject;
    public int altTable;


    public Processor(VirtualMachine machine) {
        this.machine = machine;
    }

    public void tick() {
        if (interrupted) {
            machine.interruptable.notify(interruptType, interruptedBy, interruptValue);
            interrupted = false;
            return;
        }

        byte type      = machine.loadByte(pageMap, instruction);
        byte subType   = machine.loadByte(pageMap, instruction + 1);
        byte inputType = machine.loadByte(pageMap, instruction + 2);
        byte destSize  = machine.loadByte(pageMap, instruction + 3);
        int  dest      = machine.loadInt (pageMap, instruction + 4);
        byte src1Size  = machine.loadByte(pageMap, instruction + 8);
        int  src1      = machine.loadInt (pageMap, instruction + 9);
        byte src2Size  = machine.loadByte(pageMap, instruction + 13);
        int  src2      = machine.loadInt (pageMap, instruction + 14);
        int instructionSize = 18;

        int startInstruction = instruction;
        core.Instruction.InputType inType = core.Instruction.InputType.values()[inputType];

        switch (core.Instruction.Type.values()[type]) {
            case Logic -> {
                int src1Val;
                int src2Val;
                switch (inType) {
                    case II -> {
                        src1Val = src1;
                        src2Val = src2;
                    }
                    case AI -> {
                        src1Val = machine.loadInt(pageMap, task + src1, src1Size);
                        src2Val = src2;
                    }
                    case AA -> {
                        src1Val = machine.loadInt(pageMap, task + src1, src1Size);
                        src2Val = machine.loadInt(pageMap, task + src2, src2Size);
                    }
                    default -> throw new RuntimeException();
                }
                int result = switch (core.Instruction.LogicType.values()[subType]) {
                    case ADD -> src1Val + src2Val;
                    case SUB -> src1Val - src2Val;
                    case SLL -> src1Val << src2Val;
                    case SLT -> src1Val < src2Val ? 1 : 0;
                    case SLTU -> Integer.compareUnsigned(src1Val, src2Val) < 0 ? 1 : 0;
                    case SGT -> src1Val > src2Val ? 1 : 0;
                    case SRL -> src1Val >>> src2Val;
                    case SRA -> src1Val >> src2Val;
                    case OR -> src1Val | src2Val;
                    case AND -> src1Val & src2Val;
                    case MUL -> src1Val * src2Val;
                };
                machine.store(pageMap, task + dest, result, destSize);
            }


            case Jump -> {
                switch (core.Instruction.BranchType.values()[subType]) {
                    case REL -> {
                        int destAddr = switch (inType) {
                            case A -> machine.loadInt(pageMap, task + dest) * instructionSize;
                            case I -> dest;
                            default -> throw new RuntimeException("Doesn't support input type: " + inType);
                        };
                        instruction = instruction + destAddr;
                    }
                }
            }


            case ConditionalBranch -> {
                int src1Val = machine.loadInt(pageMap, task + src1, src1Size);
                int src2Val = switch (inType) {
                    case AI -> src2;
                    case AA -> machine.loadInt(pageMap, task + src2, src1Size);
                    default -> throw new RuntimeException("Can't handle input type: " + inType);
                };
                int addr = dest;
                int nextInstruction = 0;
                switch (core.Instruction.ConditionalBranchType.values()[subType]) {
                    case EQ -> {
                        if (src1Val == src2Val) nextInstruction = addr;
                    }
                    case NEQ -> {
                        if (src1Val != src2Val) nextInstruction = addr;
                    }
                    case LT -> {
                        if (src1Val < src2Val) nextInstruction = addr;
                    }
                    case GTE -> {
                        if (src1Val >= src2Val) nextInstruction = addr;
                    }
                    case LTU -> {
                        if (Integer.compareUnsigned(src1Val, src2Val) < 0) nextInstruction = addr;
                    }
                    case GTEU -> {
                        if (Integer.compareUnsigned(src1Val, src2Val) >= 0) nextInstruction = addr;
                    }
                    default -> throw new RuntimeException("unsupported");
                }
                instruction += nextInstruction;
            }


            case Class -> {
                int table;
                int symbol;
                if (src2Size == 0) {
                    table = this.table;
                    symbol = src1;
                } else {
                    table = machine.loadInt(pageMap, task + src1);
                    symbol = src2;
                }
                int offset = switch (core.Instruction.ClassType.values()[subType]) {
                    case SIZE -> 0;
                    case ADDR -> 4;
                };
                if (inType == core.Instruction.InputType.A) {
                    symbol = machine.loadInt(pageMap, task + symbol, 2);
                }
                int size = machine.loadInt(pageMap, table + offset + (symbol * 8));
                machine.store(pageMap, task + dest, size);
            }


            case Logician -> {
                switch (core.Instruction.LogicianType.values()[subType]) {
                    case GET_OBJECT -> machine.store(pageMap, task + dest, object);
                    case GET_TABLE -> machine.store(pageMap, task + dest, table);
                    case GET_METHOD -> machine.store(pageMap, task + dest, instruction);
                    case GET_TASK -> machine.store(pageMap, task + dest, task);
                    case GET_ALT_TASK -> machine.store(pageMap, task + dest, altTask);
                    case GET_ALT_METHOD -> machine.store(pageMap, task + dest, altInstruction);
                    case GET_ALT_OBJECT -> machine.store(pageMap, task + dest, altObject);
                    case GET_ALT_TABLE -> machine.store(pageMap, task + dest, altTable);

                    case SET_OBJECT -> object = machine.loadInt(pageMap, task + src1);
                    case SET_TABLE -> table = machine.loadInt(pageMap, task + src1);
                    case SET_ALT_TASK -> altTask = machine.loadInt(pageMap, task + src1);
                    case SET_ALT_OBJECT -> altObject = machine.loadInt(pageMap, task + src1);
                    case SET_ALT_TABLE -> altTable = machine.loadInt(pageMap, task + src1);
                    case SET_METHOD_AND_TASK -> {
                        instruction = machine.loadInt(pageMap, task + src1);
                        task = machine.loadInt(pageMap, task + src2);
                    }
                    case START_ADMIN -> {
                        int currentTask = task;
                        int currentInstruction = instruction;

                        task = altTask;
                        instruction = machine.loadInt(pageMap, currentTask + src1);

                        altTask = currentTask;
                        altInstruction = currentInstruction + instructionSize;
                    }
                    case STOP_ADMIN -> {
                        int currentTask = task;

                        task = altTask;
                        instruction = altInstruction;

                        altTask = currentTask;
                    }
                }
            }


            case Memory -> {
                int srcStart = src1;
                int destStart = dest;
                int size = src2;
                switch (core.Instruction.MemoryType.values()[subType]) {
                    case COPY -> {
                        int srcPos;
                        int destPos;
                        switch (inType) {
                            case AA -> {
                                srcPos = task + srcStart;
                                destPos = task + destStart;
                            }
                            case PA -> {
                                destPos = machine.loadInt(pageMap, task + destStart);
                                srcPos = task + srcStart;
                            }
                            case AP -> {
                                destPos = task + destStart;
                                srcPos = machine.loadInt(pageMap, task + srcStart);
                            }
                            case PP -> {
                                destPos = machine.loadInt(pageMap, task + destStart);
                                srcPos = machine.loadInt(pageMap, task + srcStart);
                                size = machine.loadInt(pageMap, task + size);
                            }
                            default -> throw new RuntimeException();
                        }
                        for (int i = 0; i < size; i++) {
                            byte toCopy = machine.loadByte(pageMap, srcPos + i);
                            machine.store(pageMap, destPos + i, toCopy);
                        }
                    }
                }
            }


            case Interrupt -> {
                core.Instruction.InterruptType interruptType = core.Instruction.InterruptType.values()[subType];
                int value = switch (inType) {
                    case I -> src1;
                    case A -> machine.loadInt(pageMap, task + src1);
                    default -> throw new RuntimeException();
                };
                interrupted = true;
                interruptedBy = instance;
                this.interruptType = interruptType;
                interruptValue = value;
            }


            case Debug -> {
                int addr = machine.loadInt(pageMap, task + src1);
                int size = machine.loadInt(pageMap, task + src2);
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
        s.interruptType = interruptType;
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
        this.interruptType = s.interruptType;
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
