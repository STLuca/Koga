package language.machine;

import core.Class;
import core.Instruction;
import language.core.Argument;
import language.core.Block;
import language.core.Compiler;
import language.core.Variable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class InstructionStatement implements Statement {

    enum InstructionType {
        ALLOCATE,
        PROXY,
        SYMBOL,
        INSTRUCTIONS,
        ADDRESS,
        ARGS,
        ADMIN,
        BLOCK,
        L, LOGIC,
        J, JUMP,
        CB,
        C, CLASS,
        M, MEMORY,
        LOGICIAN,
        I, INTERRUPT,
        D, DEBUG
    }

    enum InputType {
        R,     // register
        IL,    // immediate literal,
        AL,    // argument literal,
        LDA,   // immediate data address,
        LDS,   // immediate data size,
        ADA,   // argument data address
        ADS,   // argument data size
        LG,    // immediate generic
        AG     // argument generic
    }

    enum AdminType {
        ALLOCATE,
        PORT,
        EXIT,
        TASK,
        SCHEDULE,
        COMPLETE
    }

    InstructionType instructionType;
    List<String> arguments = new ArrayList<>();

    public InstructionStatement(String instruction) {
        instructionType = InstructionType.valueOf(instruction.toUpperCase());
    }

    public InstructionStatement(String instruction, String... arguments) {
        instructionType = InstructionType.valueOf(instruction.toUpperCase());
        this.arguments = Arrays.asList(arguments);
    }

    
    public void compile(Compiler.MethodCompiler compiler, Variable variable, Variable admin, Map<String, Argument> arguments) {
        switch (instructionType) {
            case ALLOCATE -> {
                int size = 1;
                if (this.arguments.size() % 2 != 1) throw new RuntimeException();
                for (int i = 1; i < this.arguments.size(); i+=2) {
                    InputType inType = InputType.valueOf(this.arguments.get(i));
                    // should only allow specific input types i.e. ones that give values now IL, AL, LG
                    String name = this.arguments.get(i + 1);
                    int val = resolveValue(inType, name, variable, arguments).value;
                    size *= val;
                }
                int allocated = compiler.data(size);
                variable.allocations.put(this.arguments.get(0), new Variable.Allocation(size, allocated));
                compiler.debugData(variable.name, this.arguments.get(0), allocated, size);
            }
            case PROXY -> {
                if (arguments.get(this.arguments.get(0)).type != Argument.Type.Variable) throw new RuntimeException();
                if (!variable.allocations.containsKey(this.arguments.get(1))) throw new RuntimeException();

                Variable proxy = arguments.get(this.arguments.get(0)).variable;
                int location = variable.allocations.get(this.arguments.get(1)).location();
                proxy.clazz.proxy(null, proxy, location);
            }
            case SYMBOL -> {
                Class.Symbol.Type type = Class.Symbol.Type.valueOf(this.arguments.get(0).toUpperCase());
                String[] names = new String[2];
                for (int i = 2; i < this.arguments.size(); i+=2) {
                    InputType inputType = InputType.valueOf(this.arguments.get(i).toUpperCase());
                    int index = (i / 2) - 1;
                    String input = this.arguments.get(i + 1);
                    names[index] = switch (inputType) {
                        case IL -> input;
                        case AL -> arguments.get(input).name;
                        case LG -> variable.compilableGenerics.get(input).name();
                        case AG -> {
                            String[] split = input.split("\\.");
                            Variable var = arguments.get(split[0]).variable;
                            yield var.compilableGenerics.get(split[1]).name();
                        }
                        default -> throw new RuntimeException();
                    };
                }

                int symbol;
                if (names[1] == null) {
                    symbol = compiler.symbol(type, names[0]);
                } else {
                    symbol = compiler.symbol(type, names[0], names[1]);
                }

                Argument arg = Argument.of(symbol);
                arguments.put(this.arguments.get(1), arg);
            }
            case INSTRUCTIONS -> {
                int addr;
                String name = this.arguments.get(1);
                if (variable.methodAllocations.peek().containsKey(name)) {
                    addr = variable.methodAllocations.peek().get(name).location();
                } else {
                    addr = variable.allocations.get(name).location();
                }
                compiler.view(this.arguments.get(0), addr);
            }
            case ADDRESS -> {
                String name = this.arguments.get(0);
                if (variable.methodAllocations.peek().containsKey(name)) {
                    compiler.address(variable.methodAllocations.peek().get(name).location());
                    return;
                }
                if (variable.allocations.containsKey(name)) {
                    compiler.address(variable.allocations.get(name).location());
                    return;
                }
                int address = compiler.address();
                variable.methodAllocations.peek().put(name, new Variable.Allocation(4, address));
            }
            case BLOCK -> {
                if (!arguments.containsKey(this.arguments.get(0))) throw new RuntimeException("Block doesn't exist");
                Argument obj = arguments.get(this.arguments.get(0));
                Block bm = obj.block;
                Variable caller = null;
                if (this.arguments.get(1).equals("true")) caller = variable;
                bm.execute(compiler, caller);
            }
            case ADMIN -> {
                arguments.put("admin", Argument.of(admin));

                String methodAddr = "adminMethodAddr";
                String frameDataAddr = "frameDataAddr";
                String methodSymbol = "adminMethodSymbol";

                AdminType type = AdminType.valueOf(this.arguments.get(0));
                String methodName;
                switch (type) {
                    case ALLOCATE -> {
                        methodName = "allocate";
                    }
                    case PORT -> {
                        methodName = "port";
                    }
                    case EXIT -> {
                        methodName = "exit";
                    }
                    case TASK -> {
                        methodName = "task";
                    }
                    case SCHEDULE -> {
                        methodName = "schedule";
                    }
                    case COMPLETE -> {
                        methodName = "complete";
                    }
                    default -> throw new RuntimeException();
                };
                int allocateAddr = compiler.symbol(Class.Symbol.Type.METHOD, "Administrator", methodName);
                Argument arg = Argument.of(allocateAddr);
                arguments.put(methodSymbol, arg);

                int location = compiler.data(4);
                variable.methodAllocations.peek().put(methodAddr, new Variable.Allocation(4, location));
                compiler.debugData(variable.name, methodAddr, location, 4);
                location = compiler.data(4);
                variable.methodAllocations.peek().put(frameDataAddr, new Variable.Allocation(4, location));
                compiler.debugData(variable.name, frameDataAddr, location, 4);

                new InstructionStatement("c", "ADDR", "I", "LDA", methodAddr, "AL", methodSymbol).compile(compiler, variable, admin, arguments);
                new InstructionStatement("logician", "GET_ALT_TASK", frameDataAddr).compile(compiler, variable, admin, arguments);

                for (int i = 1; i < this.arguments.size(); i++) {
                    new InstructionStatement("m", "COPY", "PA", "LDA", frameDataAddr, "LDA", this.arguments.get(i), "IL", "0d4").compile(compiler, variable, admin, arguments);
                    if (i == this.arguments.size() - 1) continue;
                    new InstructionStatement("l","ADD", "AI", "LDA", frameDataAddr, "LDA", frameDataAddr, "IL", "0d4").compile(compiler, variable, admin, arguments);
                }

                new InstructionStatement("logician", "START_ADMIN", "LDA", methodAddr).compile(compiler, variable, admin, arguments);
            }
            case L, LOGIC -> {
                Compiler.InstructionCompiler ic = compiler.instruction();
                List<String> values = this.arguments;
                ic.type(Instruction.Type.Logic);
                ic.subType(Instruction.LogicType.valueOf(values.get(0)));

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
                        src2Type = inType == Instruction.InputType.AI ? InputType.AL : InputType.ADA;
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
                Resolved rDest = resolveValue(destType, dest, variable, arguments);
                Resolved rSrc1 = resolveValue(src1Type, src1, variable, arguments);
                Resolved rSrc2 = resolveValue(src2Type, src2, variable, arguments);
                ic.dest(rDest.value(), rDest.size());
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

                Resolved address = resolveValue(addrType, addr, variable, arguments);
                switch (inType) {
                    case A -> {
                        ic.dest(address.value(), address.size());
                    }
                    case I -> {
                        ic.dest(address.value());
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
                        inType = Instruction.InputType.AA;
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
                Resolved resolvedSrc1 = resolveValue(src1Type, src1, variable, arguments);
                Resolved resolvedSrc2 = resolveValue(src2Type, src2, variable, arguments);
                Resolved resolvedAddr = resolveValue(InputType.LDA, addr, variable, arguments);
                ic.inputType(inType);
                ic.dest(resolvedAddr.value());
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
                    case 4 -> {
                        destType = InputType.LDA;
                        dest = values.get(2);
                        src1Type = InputType.AL;
                        src1 = values.get(3);
                    }
                    case 5 -> {
                        destType = InputType.LDA;
                        dest = values.get(2);
                        src1Type = InputType.LDA;
                        src1 = values.get(3);
                        src2Type = InputType.AL;
                        src2 = values.get(4);
                    }
                    case 6 -> {
                        destType = InputType.valueOf(values.get(2));
                        dest = values.get(3);
                        src1Type = InputType.valueOf(values.get(4));
                        src1 = values.get(5);
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

                Resolved resolvedDest = resolveValue(destType, dest, variable, arguments);
                Resolved resolvedTable = resolveValue(src1Type, src1, variable, arguments);
                ic.dest(resolvedDest.value(), resolvedDest.size());
                ic.src(resolvedTable.value(), resolvedTable.size());
                if (src2 != null) {
                    Resolved resolvedSymbol = resolveValue(src2Type, src2, variable, arguments);
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

                Resolved resolvedDest = resolveValue(destType, dest, variable, arguments);
                Resolved resolvedSrc = resolveValue(srcType, src, variable, arguments);
                Resolved resolvedSize = resolveValue(sizeType, size, variable, arguments);
                ic.dest(resolvedDest.value(), resolvedDest.size());
                ic.src(resolvedSrc.value(), resolvedSrc.size());
                ic.src(resolvedSize.value(), resolvedSize.size());
            }
            case I, INTERRUPT -> {
                Compiler.InstructionCompiler ic = compiler.instruction();
                List<String> values = this.arguments;
                ic.type(Instruction.Type.Interrupt);
                ic.subType(Instruction.InterruptType.valueOf(values.get(0)));
                ic.inputType(Instruction.InputType.valueOf(values.get(1)));
                String src = values.get(2);
                Resolved resolvedSrc = resolveValue(InputType.LDA, src, variable, arguments);
                ic.src(resolvedSrc.value(), resolvedSrc.size());
            }
            case D, DEBUG -> {
                Compiler.InstructionCompiler ic = compiler.instruction();
                List<String> values = this.arguments;
                ic.type(Instruction.Type.Debug);
                ic.subType(Instruction.DebugType.valueOf(values.get(0)));
                String src = values.get(1);
                String size = values.get(2);
                Resolved resolvedSrc = resolveValue(InputType.ADA, src, variable, arguments);
                Resolved resolvedSize = resolveValue(InputType.ADA, size, variable, arguments);
                ic.src(resolvedSrc.value(), resolvedSrc.size());
                ic.src(resolvedSize.value(), resolvedSize.size());
            }
            case LOGICIAN -> {
                Compiler.InstructionCompiler ic = compiler.instruction();
                List<String> values = this.arguments;
                ic.type(Instruction.Type.Logician);
                Instruction.LogicianType subType = Instruction.LogicianType.valueOf(values.get(0));
                ic.subType(subType);
                switch (subType) {
                    case GET_OBJECT, GET_TABLE, GET_METHOD, GET_TASK, GET_ALT_TASK, GET_ALT_METHOD, GET_ALT_OBJECT, GET_ALT_TABLE -> {
                        switch (values.size()) {
                            case 2 -> {
                                Resolved resolvedSrc = resolveValue(InputType.LDA, values.get(1), variable, arguments);
                                ic.dest(resolvedSrc.value, resolvedSrc.size);
                            }
                            case 3 -> {
                                InputType srcType = InputType.valueOf(values.get(1));
                                Resolved resolvedSrc = resolveValue(srcType, values.get(2), variable, arguments);
                                ic.dest(resolvedSrc.value, resolvedSrc.size);
                            }
                            default -> throw new RuntimeException();
                        }

                    }
                    case SET_OBJECT, SET_TABLE, START_ADMIN, SET_ALT_TASK, SET_ALT_OBJECT, SET_ALT_TABLE -> {
                        InputType srcType = InputType.valueOf(values.get(1));
                        Resolved resolvedSrc = resolveValue(srcType, values.get(2), variable, arguments);
                        ic.src(resolvedSrc.value, resolvedSrc.size);
                    }
                    case SET_METHOD_AND_TASK -> {
                        InputType src1Type = InputType.valueOf(values.get(1));
                        Resolved resolvedSrc1 = resolveValue(src1Type, values.get(2), variable, arguments);
                        InputType src2Type = InputType.valueOf(values.get(3));
                        Resolved resolvedSrc2 = resolveValue(src2Type, values.get(4), variable, arguments);
                        ic.src(resolvedSrc1.value, resolvedSrc1.size);
                        ic.src(resolvedSrc2.value, resolvedSrc2.size);
                    }
                    case STOP_ADMIN -> {}
                    default -> throw new RuntimeException();
                }
            }
        }
    }

    static int parseLiteral(String v) {
        if (v.startsWith("0d")) {
            return Integer.parseInt(v.substring(2));
        }
        if (v.equals("true")) return 1;
        if (v.equals("false")) return 0;
        if (v.matches("'[a-zA-Z]'")) return v.charAt(1);
        try {
            return Integer.parseInt(v);
        } catch (RuntimeException ignored) {}
        throw new RuntimeException("Not implemented");
    }

    record Resolved(int size, int value) {}
    static Resolved resolveValue(InputType type, String toResolve, Variable variable, Map<String, Argument> arguments) {
        switch (type) {
            case R -> {
                switch (toResolve) {
                    case "altTable" -> {
                        return new Resolved(7, 4);
                    }
                }
            }
            case IL -> {
                return new Resolved(4, parseLiteral(toResolve));
            }
            case AL -> {
                // arguments should be type literal
                Argument arg = arguments.get(toResolve);
                if (arg.type != Argument.Type.Literal) throw new RuntimeException();
                return new Resolved(4, arg.val);
            }
            case LDA -> {
                // Local allocation
                if (variable.methodAllocations.peek().containsKey(toResolve)) {
                    Variable.Allocation allocation = variable.methodAllocations.peek().get(toResolve);
                    return new Resolved(allocation.size(), allocation.location());
                }
                Variable.Allocation allocation = variable.allocations.get(toResolve);
                return new Resolved(allocation.size(), allocation.location());
            }
            case ADA -> {
                // argument should be type variable
                String[] split = toResolve.split("\\.");
                Argument arg = arguments.get(split[0]);
                if (arg.type != Argument.Type.Variable) throw new RuntimeException();
                if (split.length == 2) {
                    // we just want an allocation
                    Variable.Allocation allocation = arg.variable.allocations.get(split[1]);
                    return new Resolved(allocation.size(), allocation.location());
                }
                // we want the variable
                int start = Integer.MAX_VALUE;
                int size = 0;
                for (Variable.Allocation a : arg.variable.allocations.values()) {
                    if (a.location() < start) start = a.location();
                    size += a.size();
                }
                return new Resolved(size, start);
            }
            case LDS -> {
                // local size
                if (variable.methodAllocations.peek().containsKey(toResolve)) {
                    return new Resolved(4, variable.methodAllocations.peek().get(toResolve).size());
                }
                return new Resolved(4, variable.allocations.get(toResolve).size());
            }
            case ADS -> {// argument should be type variable
                String[] split = toResolve.split("\\.");
                Argument arg = arguments.get(split[0]);
                if (arg.type != Argument.Type.Variable) throw new RuntimeException();
                if (split.length == 2) {
                    // we just want an allocation
                    return new Resolved(4, arg.variable.allocations.get(split[1]).size());
                }
                // we want the variable
                int size = 0;
                for (Variable.Allocation a : arg.variable.allocations.values()) {
                    size += a.size();
                }
                return new Resolved(4, size);
            }
            case LG -> {
                if (variable.compilableGenerics.containsKey(toResolve)) {
                    throw new RuntimeException();
                    // return new Resolved(variable.compilableGenerics2.get(toResolve).name(), 4);
                } else if (variable.generics.containsKey(toResolve)) {
                    return new Resolved(4, variable.generics.get(toResolve).size());
                } else {
                    // Not a concrete class
                }
            }
            case AG -> {
                String[] split = toResolve.split("\\.");
                Variable var = arguments.get(split[0]).variable;
                if (var.compilableGenerics.containsKey(split[1])) {
                    throw new RuntimeException();
                    // return new Resolved(variable.compilableGenerics2.get(toResolve).name(), 4);
                } else if (var.generics.containsKey(split[1])) {
                    return new Resolved(4, variable.generics.get(split[1]).size());
                } else {
                    // Not a concrete class
                }
            }
        }
        throw new RuntimeException();
    }

}
