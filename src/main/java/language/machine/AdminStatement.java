package language.machine;

import core.Types;
import language.core.*;

import java.util.ArrayList;
import java.util.Map;

public class AdminStatement implements Statement {

    enum AdminType {
        ALLOCATE,
        PORT,
        EXIT,
        TASK,
        AWAIT_TASK,
        TRANSITION
    }

    ArrayList<String> arguments = new ArrayList<>();
    
    public void compile(Compiler.MethodCompiler compiler, Sources sources, Variable variable, Map<String, Argument> arguments, Context context) {
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
            case AWAIT_TASK -> {
                methodName = "awaitTask";
            }
            case TRANSITION -> {
                methodName = "transition";
            }
            default -> throw new RuntimeException();
        };
        int allocateAddr = compiler.symbol(Types.Symbol.METHOD, "core.Administrator", methodName);
        Argument arg = Argument.of(allocateAddr);
        arguments.put(methodSymbol, arg);

        int location = compiler.data(4);
        Context.Allocation allocation = new Context.Allocation(4, location);
        context.add(methodAddr, allocation);
        compiler.debugData(context.stateName(variable.name), methodAddr, location, 4);
        location = compiler.data(4);
        allocation = new Context.Allocation(4, location);
        context.add(frameDataAddr, allocation);
        compiler.debugData(context.stateName(variable.name), frameDataAddr, location, 4);

        new InstructionStatement("c", "ADDR", "LI", "LDA", methodAddr, "R", "table", "AL", methodSymbol).compile(compiler, sources, variable, arguments, context);
        new InstructionStatement("i", "ADD", "LI", "LDA", frameDataAddr, "R", "altTask", "IL", "0d0").compile(compiler, sources, variable, arguments, context);

        for (int i = 1; i < this.arguments.size(); i++) {
            new InstructionStatement("m", "COPY", "TII", "LDA", frameDataAddr, "LDA", this.arguments.get(i), "IL", "0d4").compile(compiler, sources, variable, arguments, context);
            if (i == this.arguments.size() - 1) continue;
            new InstructionStatement("i","ADD", "TI", "LDA", frameDataAddr, "LDA", frameDataAddr, "IL", "0d4").compile(compiler, sources, variable, arguments, context);
        }

        new InstructionStatement("logician", "START_ADMIN", "T", "LDA", methodAddr).compile(compiler, sources, variable, arguments, context);
    }

}
