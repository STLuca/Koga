package language.machine;

import core.Types;
import language.core.*;

import java.util.ArrayList;

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
    
    public void compile(Compiler.MethodCompiler compiler, Sources sources, Scope variable, Scope scope) {
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
        scope.literals.put(methodSymbol, allocateAddr);

        int location = compiler.data(4);
        Scope.Allocation allocation = new Scope.Allocation(4, location);
        scope.add(methodAddr, allocation);
        compiler.debugData(scope.stateName(variable.name), methodAddr, location, 4);
        location = compiler.data(4);
        allocation = new Scope.Allocation(4, location);
        scope.add(frameDataAddr, allocation);
        compiler.debugData(scope.stateName(variable.name), frameDataAddr, location, 4);

        new InstructionStatement("c", "ADDR", "LI", "LDA", methodAddr, "R", "table", "AL", methodSymbol).compile(compiler, sources, variable, scope);
        new InstructionStatement("i", "ADD", "LI", "LDA", frameDataAddr, "R", "altTask", "IL", "0d0").compile(compiler, sources, variable, scope);

        for (int i = 1; i < this.arguments.size(); i++) {
            new InstructionStatement("m", "COPY", "TII", "LDA", frameDataAddr, "LDA", this.arguments.get(i), "IL", "0d4").compile(compiler, sources, variable, scope);
            if (i == this.arguments.size() - 1) continue;
            new InstructionStatement("i","ADD", "TI", "LDA", frameDataAddr, "LDA", frameDataAddr, "IL", "0d4").compile(compiler, sources, variable, scope);
        }

        new InstructionStatement("logician", "START_ADMIN", "T", "LDA", methodAddr).compile(compiler, sources, variable, scope);
    }

}
