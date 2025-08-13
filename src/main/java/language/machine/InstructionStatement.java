package language.machine;

import core.Instruction;
import language.core.*;
import language.machine.InputType.Resolved;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class InstructionStatement implements Statement {

    enum InstructionType {
        I, INTEGER,
        J, JUMP,
        CB,
        C, CLASS,
        M, MEMORY,
        LOGICIAN,
        D, DEBUG,
        MATH,
        A, ATOMIC,
        V, VECTOR,
    }

    InstructionType instructionType;
    ArrayList<String> arguments = new ArrayList<>();

    public InstructionStatement(String instruction) {
        instructionType = InstructionType.valueOf(instruction.toUpperCase());
    }

    public InstructionStatement(String instruction, String... arguments) {
        instructionType = InstructionType.valueOf(instruction.toUpperCase());
        this.arguments = new ArrayList<>(Arrays.asList(arguments));
    }

    public void compile(Compiler.MethodCompiler compiler, Sources sources, Variable variable, Map<String, Argument> arguments, Context context) {
        switch (instructionType) {
            case I, INTEGER -> {
                Compiler.InstructionCompiler ic = compiler.instruction();
                List<String> values = this.arguments;
                ic.type(Instruction.Type.Integer);
                ic.subType(Instruction.IntegerType.valueOf(values.get(0)));

                Instruction.InputType inType = Instruction.InputType.valueOf(values.get(1));
                ic.inputType(inType);

                InputType destType;
                String dest;
                InputType src1Type;
                String src1;
                InputType src2Type;
                String src2;
                switch (values.size()) {
                    case 5 -> {
                        destType = InputType.LDA;
                        dest = values.get(2);
                        src1Type = InputType.LDA;
                        src1 = values.get(3);
                        src2Type = inType == Instruction.InputType.TI ? InputType.AL : InputType.ADA;
                        src2 = values.get(4);
                    }
                    case 8 -> {
                        destType = InputType.valueOf(values.get(2));
                        dest = values.get(3);
                        src1Type = InputType.valueOf(values.get(4));
                        src1 = values.get(5);
                        src2Type = InputType.valueOf(values.get(6));
                        src2 = values.get(7);
                    }
                    default -> throw new IllegalArgumentException();
                }
                Resolved rDest = destType.resolve(dest, variable, arguments, context);
                Resolved rSrc1 = src1Type.resolve(src1, variable, arguments, context);
                Resolved rSrc2 = src2Type.resolve(src2, variable, arguments, context);
                ic.src(rDest.value(), rDest.size());
                ic.src(rSrc1.value(), rSrc1.size());
                ic.src(rSrc2.value(), rSrc2.size());
            }
            case MATH -> {
                Compiler.InstructionCompiler ic = compiler.instruction();
                List<String> values = this.arguments;
                ic.type(Instruction.Type.Math);
                ic.subType(Instruction.MathType.valueOf(values.get(0)));

                Instruction.InputType inType = Instruction.InputType.valueOf(values.get(1));
                ic.inputType(inType);

                InputType destType;
                String dest;
                InputType src1Type;
                String src1;
                InputType src2Type;
                String src2;
                switch (values.size()) {
                    case 5 -> {
                        destType = InputType.LDA;
                        dest = values.get(2);
                        src1Type = InputType.LDA;
                        src1 = values.get(3);
                        src2Type = inType == Instruction.InputType.TI ? InputType.AL : InputType.ADA;
                        src2 = values.get(4);
                    }
                    case 8 -> {
                        destType = InputType.valueOf(values.get(2));
                        dest = values.get(3);
                        src1Type = InputType.valueOf(values.get(4));
                        src1 = values.get(5);
                        src2Type = InputType.valueOf(values.get(6));
                        src2 = values.get(7);
                    }
                    default -> throw new IllegalArgumentException();
                }
                Resolved rDest = destType.resolve(dest, variable, arguments, context);
                Resolved rSrc1 = src1Type.resolve(src1, variable, arguments, context);
                Resolved rSrc2 = src2Type.resolve(src2, variable, arguments, context);
                ic.src(rDest.value(), rDest.size());
                ic.src(rSrc1.value(), rSrc1.size());
                ic.src(rSrc2.value(), rSrc2.size());
            }
            case J, JUMP -> {
                Compiler.InstructionCompiler ic = compiler.instruction();
                List<String> values = this.arguments;
                ic.type(Instruction.Type.Jump);
                ic.subType(Instruction.BranchType.valueOf(values.get(0)));

                Instruction.InputType inType = Instruction.InputType.valueOf(values.get(1));
                ic.inputType(inType);

                InputType addrType;
                String addr;
                switch (values.size()) {
                    case 3 -> {
                        addrType = InputType.LDA;
                        addr = values.get(2);
                    }
                    case 4 -> {
                        addrType = InputType.valueOf(values.get(2));
                        addr = values.get(3);
                    }
                    default -> throw new RuntimeException();
                }

                Resolved address = addrType.resolve(addr, variable, arguments, context);
                switch (inType) {
                    case T -> {
                        ic.src(address.value(), address.size());
                        ic.src(address.value(), address.size());
                    }
                    case I -> {
                        ic.src(address.value());
                        ic.src(address.value());
                    }
                    default -> throw new RuntimeException();
                }
            }
            case CB -> {
                Compiler.InstructionCompiler ic = compiler.instruction();
                List<String> values = this.arguments;
                ic.type(Instruction.Type.ConditionalBranch);
                ic.subType(Instruction.ConditionalBranchType.valueOf(values.get(0)));
                Instruction.InputType inType;
                InputType src1Type;
                String src1;
                InputType src2Type;
                String src2;
                String addr;
                switch (values.size()) {
                    case 6 -> {
                        inType = Instruction.InputType.TT;
                        src1Type = InputType.valueOf(values.get(1));
                        src1 = values.get(2);
                        src2Type = InputType.valueOf(values.get(3));
                        src2 = values.get(4);
                        addr = values.get(5);
                    }
                    case 7 -> {
                        inType = Instruction.InputType.valueOf(values.get(1));
                        src1Type = InputType.valueOf(values.get(2));
                        src1 = values.get(3);
                        src2Type = InputType.valueOf(values.get(4));
                        src2 = values.get(5);
                        addr = values.get(6);
                    }
                    default -> throw new RuntimeException("Bad number of values");
                }
                Resolved resolvedSrc1 = src1Type.resolve(src1, variable, arguments, context);
                Resolved resolvedSrc2 = src2Type.resolve(src2, variable, arguments, context);
                Resolved resolvedAddr = InputType.LDA.resolve(addr, variable, arguments, context);
                if (resolvedAddr.value() == -1) {
                    // Put a placeholder address. Things will probably not work if the addr isn't then updated
                    int address = compiler.address();
                    Variable.Allocation addrAllocation = new Variable.Allocation(4, address);
                    variable.methodAllocations.peek().put(addr, addrAllocation);
                    resolvedAddr = new Resolved(addrAllocation.size(), addrAllocation.location());
                }
                ic.inputType(inType);
                ic.src(resolvedAddr.value());
                ic.src(resolvedSrc1.value(), resolvedSrc1.size());
                ic.src(resolvedSrc2.value(), resolvedSrc2.size());
            }
            case C, CLASS -> {
                Compiler.InstructionCompiler ic = compiler.instruction();
                List<String> values = this.arguments;
                ic.type(Instruction.Type.Class);
                ic.subType(Instruction.ClassType.valueOf(values.get(0)));
                ic.inputType(Instruction.InputType.valueOf(values.get(1)));
                InputType destType;
                String dest;
                InputType src1Type;
                String src1;
                InputType src2Type = null;
                String src2 = null;
                switch (values.size()) {
                    case 5 -> {
                        destType = InputType.LDA;
                        dest = values.get(2);
                        src1Type = InputType.R;
                        src1 = values.get(3);
                        src2Type = InputType.AL;
                        src2 = values.get(4);
                    }
                    case 8 -> {
                        destType = InputType.valueOf(values.get(2));
                        dest = values.get(3);
                        src1Type = InputType.valueOf(values.get(4));
                        src1 = values.get(5);
                        src2Type = InputType.valueOf(values.get(6));
                        src2 = values.get(7);
                    }
                    default -> throw new RuntimeException();
                }

                Resolved resolvedDest = destType.resolve(dest, variable, arguments, context);
                Resolved resolvedTable = src1Type.resolve(src1, variable, arguments, context);
                ic.src(resolvedDest.value(), resolvedDest.size());
                ic.src(resolvedTable.value(), resolvedTable.size());
                if (src2 != null) {
                    Resolved resolvedSymbol = src2Type.resolve(src2, variable, arguments, context);
                    ic.src(resolvedSymbol.value(), resolvedSymbol.size());
                }
            }
            case M, MEMORY -> {
                Compiler.InstructionCompiler ic = compiler.instruction();
                List<String> values = this.arguments;
                ic.type(Instruction.Type.Memory);
                ic.subType(Instruction.MemoryType.valueOf(values.get(0)));
                ic.inputType(Instruction.InputType.valueOf(values.get(1)));
                InputType destType = InputType.valueOf(values.get(2));
                String dest = values.get(3);
                InputType srcType = InputType.valueOf(values.get(4));
                String src = values.get(5);
                InputType sizeType = InputType.valueOf(values.get(6));
                String size = values.get(7);

                Resolved resolvedDest = destType.resolve(dest, variable, arguments, context);
                Resolved resolvedSrc = srcType.resolve(src, variable, arguments, context);
                Resolved resolvedSize = sizeType.resolve(size, variable, arguments, context);
                ic.src(resolvedDest.value(), resolvedDest.size());
                ic.src(resolvedSrc.value(), resolvedSrc.size());
                ic.src(resolvedSize.value(), resolvedSize.size());
            }
            case D, DEBUG -> {
                Compiler.InstructionCompiler ic = compiler.instruction();
                List<String> values = this.arguments;
                ic.type(Instruction.Type.Debug);
                ic.subType(Instruction.DebugType.valueOf(values.get(0)));
                String src = values.get(1);
                String size = values.get(2);
                Resolved resolvedSrc = InputType.ADA.resolve(src, variable, arguments, context);
                Resolved resolvedSize = InputType.ADA.resolve(size, variable, arguments, context);
                ic.src(0, 0);
                ic.src(resolvedSrc.value(), resolvedSrc.size());
                ic.src(resolvedSize.value(), resolvedSize.size());
            }
            case LOGICIAN -> {
                Compiler.InstructionCompiler ic = compiler.instruction();
                List<String> values = this.arguments;
                ic.type(Instruction.Type.Logician);
                Instruction.LogicianType subType = Instruction.LogicianType.valueOf(values.get(0));
                ic.subType(subType);
                ic.src(0, 0);
                switch (subType) {
                    case SET_OBJECT, SET_TABLE, START_ADMIN, SET_ALT_TASK, SET_ALT_OBJECT, SET_ALT_TABLE -> {
                        ic.inputType(Instruction.InputType.valueOf(values.get(1)));
                        InputType srcType = InputType.valueOf(values.get(2));
                        Resolved resolvedSrc = srcType.resolve(values.get(3), variable, arguments, context);

                        ic.src(resolvedSrc.value(), resolvedSrc.size());
                    }
                    case SET, SET_METHOD_AND_TASK -> {
                        ic.inputType(Instruction.InputType.valueOf(values.get(1)));
                        InputType src1Type = InputType.valueOf(values.get(2));
                        Resolved resolvedSrc1 = src1Type.resolve(values.get(3), variable, arguments, context);
                        InputType src2Type = InputType.valueOf(values.get(4));
                        Resolved resolvedSrc2 = src2Type.resolve(values.get(5), variable, arguments, context);
                        ic.src(resolvedSrc1.value(), resolvedSrc1.size());
                        ic.src(resolvedSrc2.value(), resolvedSrc2.size());
                    }
                    case STOP_ADMIN -> {}
                    case NOTIFY_SUPERVISOR -> {
                        ic.inputType(Instruction.InputType.valueOf(values.get(1)));
                        String src = values.get(2);
                        Resolved resolvedSrc = InputType.LDA.resolve(src, variable, arguments, context);
                        ic.src(resolvedSrc.value(), resolvedSrc.size());
                    }
                    default -> throw new RuntimeException();
                }
            }
        }
    }


}
