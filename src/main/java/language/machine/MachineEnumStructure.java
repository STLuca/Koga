package language.machine;

import language.core.*;
import language.core.Compiler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MachineEnumStructure implements Structure {

    static class Literal {
        String name;
        String value;
    }

    String name;
    ArrayList<Literal> literals = new ArrayList<>();
    Data data;
    
    public String name() {
        return name;
    }
    
    public int size(Sources sources) {
        return data.size;
    }
    
    public void declare(Compiler.MethodCompiler compiler, Sources sources, Scope scope, String name, List<String> generics) {
        Scope variable = scope.add(name);
        variable.name = name;
        variable.structure = this;

        int allocation = compiler.data(data.size);
        variable.allocations.put(data.name, new Scope.Allocation(data.size, allocation));
        compiler.debugData(variable.stateName(data.name), data.name, allocation, data.size);
    }
    
    public void proxy(Sources sources, Scope variable, int location) {
        throw new RuntimeException("Not supported");
    }
    
    public void construct(Compiler.MethodCompiler compiler, Sources sources, Scope scope, String name, List<String> generics, String constructorName, List<Argument> arguments) {
        Scope variable = scope.add(name);
        variable.name = name;
        variable.structure = this;

        // Read literal from args
        if (arguments.size() != 1) throw new RuntimeException("Only 1 argument allowed for enum constructor");
        if (arguments.get(0).type != Argument.Type.Name) throw new RuntimeException("Argument must be a name");
        String literalName = arguments.get(0).name;
        String literal = null;
        for (Literal l : literals) {
            if (l.name.equals(literalName)) {
                literal = l.value;
                break;
            }
        }
        if (literal == null) throw new RuntimeException("Literal " + literalName + " doesn't exist");

        // data
        int allocation = compiler.data(data.size);
        variable.allocations.put(data.name, new Scope.Allocation(data.size, allocation));
        compiler.debugData(variable.stateName(data.name), data.name, allocation, data.size);

        // instructions
        // l(ADD, II, LDA, data.name, IL, 0d0, IL, literal);
        // compiler.instruction("l", "ADD", "II", "LDA", data.name, "IL", "0d0", "IL", literal);
        Scope operationScope = variable.startOperation(constructorName);
        new InstructionStatement("i", "ADD", "II", "LDA", data.name, "IL", "0d0", "IL", literal).compile(compiler, sources, variable, Map.of(), operationScope);
    }
    
    public void operate(Compiler.MethodCompiler compiler, Sources sources, Scope scope, Scope variable, String operationName, List<Argument> arguments) {
        if (!operationName.equals("match")) throw new RuntimeException("expecting method match");
        if (arguments.size() == 0) throw new RuntimeException("expecting arguments");
        if (arguments.size() % 2 != 0) throw new RuntimeException("expecting even number of arguments");

        Scope operationScope = variable.startOperation(operationName);
        int endAddr = compiler.address();
        Scope.Allocation allocation = new Scope.Allocation(4, endAddr);
        operationScope.add("end", allocation);

        int i = 0;
        while(i < arguments.size()) {
            Argument literalArg = arguments.get(i);
            Argument blockArg = arguments.get(i + 1);
            if (literalArg.type != Argument.Type.Name) throw new RuntimeException("expecting arg " + i + " to be name");
            if (blockArg.type != Argument.Type.Block) throw new RuntimeException("expecting arg " + (i + 1) + " to be block");

            String literalName = literalArg.name;
            String literal = null;
            for (Literal l : literals) {
                if (l.name.equals(literalName)) {
                    literal = l.value;
                    break;
                }
            }
            if (literal == null) throw new RuntimeException("Literal " + literalName + " doesn't exist");
            Block block = blockArg.block;

            // cb(NEQ, AI, LDA, val, IL, literal, instruction);
            // block;
            // j(REL, I, END);
            // Addr instruction;
            String instruction = "after" + i;
            int addr = compiler.address();
            Scope.Allocation afterAllocation = new Scope.Allocation(4, addr);
            operationScope.add(instruction, afterAllocation);
            new InstructionStatement("cb", "NEQ", "TI", "LDA", "val", "IL", literal, instruction).compile(compiler, sources, variable, Map.of(), operationScope);
            block.execute(compiler, operationScope);
            new InstructionStatement("j", "REL", "I", "end").compile(compiler, sources, variable, Map.of(), operationScope);

            compiler.address(addr);

            i += 2;
        }
        compiler.address(endAddr);
    }

}
