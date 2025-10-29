package language.machine;

import language.core.*;
import language.core.Compiler;

import java.util.ArrayList;
import java.util.HashMap;
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
    
    public void declare(Compiler.MethodCompiler compiler, Sources sources, Map<String, Variable> variables, String name, List<String> generics) {
        Variable variable = new Variable();
        variable.name = name;
        variable.structure = this;
        variables.put(name, variable);
        int allocation = compiler.data(data.size);
        variable.allocations.put(data.name, new Variable.Allocation(data.size, allocation));
        compiler.debugData(variable.name, data.name, allocation, data.size);
    }
    
    public void proxy(Sources sources, Variable variable, int location) {
        throw new RuntimeException("Not supported");
    }
    
    public void construct(Compiler.MethodCompiler compiler, Sources sources, Map<String, Variable> variables, String name, List<String> generics, String constructorName, List<Argument> arguments, Context context) {
        Variable variable = new Variable();
        variable.name = name;
        variable.structure = this;
        variables.put(name, variable);
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
        variable.allocations.put(data.name, new Variable.Allocation(data.size, allocation));
        compiler.debugData(variable.name, data.name, allocation, data.size);

        // instructions
        // l(ADD, II, LDA, data.name, IL, 0d0, IL, literal);
        // compiler.instruction("l", "ADD", "II", "LDA", data.name, "IL", "0d0", "IL", literal);
        variable.methodAllocations.push(new HashMap<>());
        new InstructionStatement("i", "ADD", "II", "LDA", data.name, "IL", "0d0", "IL", literal).compile(compiler, sources, variable, Map.of(), context);

        variable.methodAllocations.pop();
    }
    
    public void operate(Compiler.MethodCompiler compiler, Sources sources, Map<String, Variable> variables, Variable variable, String operationName, List<Argument> arguments, Context context) {
        if (!operationName.equals("match")) throw new RuntimeException("expecting method match");
        if (arguments.size() == 0) throw new RuntimeException("expecting arguments");
        if (arguments.size() % 2 != 0) throw new RuntimeException("expecting even number of arguments");

        variable.methodAllocations.push(new HashMap<>());
        int endAddr = compiler.address();
        variable.methodAllocations.peek().put("end", new Variable.Allocation(4, endAddr));

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
            variable.methodAllocations.peek().put(instruction, new Variable.Allocation(4, addr));
            new InstructionStatement("cb", "NEQ", "TI", "LDA", "val", "IL", literal, instruction).compile(compiler, sources, variable, Map.of(), context);
            block.execute(compiler);
            new InstructionStatement("j", "REL", "I", "end").compile(compiler, sources, variable, Map.of(), context);

            compiler.address(addr);

            i += 2;
        }
        compiler.address(endAddr);
        variable.methodAllocations.pop();
    }

}
