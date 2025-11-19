package language.machine;

import language.core.*;
import language.core.Compiler;

import java.util.ArrayList;
import java.util.List;

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
    
    public int size(Repository repository) {
        return data.size;
    }
    
    public void declare(Compiler.MethodCompiler compiler, Repository repository, Scope scope, String name, List<GenericArgument> generics) {
        Scope variable = scope.state(this, name);

        int allocation = compiler.data(data.size);
        variable.put(data.name, new Scope.Allocation(data.size, allocation));
    }
    
    public void proxy(Repository repository, Scope variable, int location) {
        throw new RuntimeException("Not supported");
    }
    
    public void construct(Compiler.MethodCompiler compiler, Repository repository, Scope scope, String name, List<GenericArgument> generics, String constructorName, List<String> argumentNames) {
        Scope variable = scope.state(this, name);

        // Read literal from args
        if (argumentNames.size() != 1) throw new RuntimeException("Only 1 argument allowed for enum constructor");
        String literalName = argumentNames.get(0);
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
        variable.put(data.name, new Scope.Allocation(data.size, allocation));

        // instructions
        // l(ADD, II, P, data.name, I, 0d0, I, literal);
        // compiler.instruction("l", "ADD", "II", "P", data.name, "I", "0d0", "I", literal);
        Scope operationScope = scope.startOperation(variable, constructorName);
        new InstructionStatement("i", "ADD", "II", "P", data.name, "I", "0d0", "I", literal).compile(compiler, repository, variable, operationScope);
    }
    
    public void operate(Compiler.MethodCompiler compiler, Repository repository, Scope scope, Scope variable, String operationName, List<String> arguments) {
        if (!operationName.equals("match")) throw new RuntimeException("expecting method match");
        if (arguments.isEmpty()) throw new RuntimeException("expecting arguments");
        if (arguments.size() % 2 != 0) throw new RuntimeException("expecting even number of arguments");

        Scope operationScope = scope.startOperation(variable, operationName);
        int endAddr = compiler.address();
        Scope.Allocation allocation = new Scope.Allocation(4, endAddr);
        operationScope.put("end", allocation);

        int i = 0;
        while(i < arguments.size()) {
            String literalArg = arguments.get(i);
            String blockArg = arguments.get(i + 1);
            Scope.Block block = scope.findBlock(blockArg).orElse(null);
            if (block == null) throw new RuntimeException("expecting arg " + (i + 1) + " to be block");

            String literalName = literalArg;
            String literal = null;
            for (Literal l : literals) {
                if (l.name.equals(literalName)) {
                    literal = l.value;
                    break;
                }
            }
            if (literal == null) throw new RuntimeException("Literal " + literalName + " doesn't exist");


            // cb(NEQ, AI, P, val, I, literal, instruction);
            // block;
            // j(REL, I, END);
            // Addr instruction;
            String instruction = "after" + i;
            int addr = compiler.address();
            Scope.Allocation afterAllocation = new Scope.Allocation(4, addr);
            operationScope.put(instruction, afterAllocation);
            new InstructionStatement("cb", "NEQ", "TI", "P", "val", "I", literal, instruction).compile(compiler, repository, variable, operationScope);
            block.execute(compiler, operationScope);
            new InstructionStatement("j", "REL", "I", "end").compile(compiler, repository, variable, operationScope);

            compiler.address(addr);

            i += 2;
        }
        compiler.address(endAddr);
    }

}
