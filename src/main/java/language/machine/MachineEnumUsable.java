package language.machine;

import language.core.*;
import language.core.Compiler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MachineEnumUsable implements Usable {

    static class Literal {
        String name;
        String value;
    }

    static {
        InstructionStatement is = new InstructionStatement("l");
    }
//    compiler.instruction(InstructionStatementHelper.instruction(variable, Map.of(),
//                "l", "ADD", "II", "LDA", data.name, "IL", "0d0", "IL", literal));

    String name;
    List<Literal> literals = new ArrayList<>();
    Data data;

    
    public String name() {
        return name;
    }

    
    public int size() {
        return data.size;
    }

    
    public void declare(Compiler.MethodCompiler compiler, Classes classes, Variable variable, List<String> generics) {
        compiler.pushContext();
        int allocation = compiler.data(data.size);
        variable.allocations.put(data.name, new Variable.Allocation(data.size, allocation));
        compiler.debugData(variable.name, data.name, allocation, data.size);
        compiler.popContext();
    }

    
    public void proxy(Classes classes, Variable variable, int location) {
        throw new RuntimeException("Not supported");
    }

    
    public void construct(Compiler.MethodCompiler compiler, Classes classes, Map<String, Variable> variables, Variable variable, List<String> generics, String constructorName, List<Argument> arguments) {
        // Read literal from args
        if (arguments.size() != 1) throw new RuntimeException("Only 1 argument allowed for enum constructor");
        if (arguments.get(0).type != Argument.Type.Name) throw new RuntimeException("Argument must be a name");
        String literalName = arguments.get(0).name;
        String literal = literals.stream().filter(l -> l.name.equals(literalName)).map(l -> l.value).findFirst().orElse(null);
        if (literal == null) throw new RuntimeException("Literal " + literalName + " doesn't exist");
        Variable admin = variables.get("admin");

        compiler.pushContext();

        // data
        int allocation = compiler.data(data.size);
        variable.allocations.put(data.name, new Variable.Allocation(data.size, allocation));
        compiler.debugData(variable.name, data.name, allocation, data.size);

        // instructions
        // l(ADD, II, LDA, data.name, IL, 0d0, IL, literal);
        // compiler.instruction("l", "ADD", "II", "LDA", data.name, "IL", "0d0", "IL", literal);
        variable.methodAllocations.push(new HashMap<>());
        new InstructionStatement("l", "ADD", "II", "LDA", data.name, "IL", "0d0", "IL", literal).compile(compiler, variable, admin, Map.of());


        variable.methodAllocations.pop();

        compiler.popContext();
    }

    
    public void invoke(Compiler.MethodCompiler compiler, Map<String, Variable> variables, Variable variable, String methodName, List<Argument> arguments) {
        if (!methodName.equals("match")) throw new RuntimeException("expecting method match");
        if (arguments.size() == 0) throw new RuntimeException("expecting arguments");
        if (arguments.size() % 2 != 0) throw new RuntimeException("expecting even number of arguments");
        Variable admin = variables.get("admin");

        compiler.pushContext();
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
            String literal = literals.stream().filter(l -> l.name.equals(literalName)).map(l -> l.value).findFirst().orElse(null);
            if (literal == null) throw new RuntimeException("Literal " + literalName + " doesn't exist");
            Block block = blockArg.block;

            // cb(NEQ, AI, LDA, val, IL, literal, instruction);
            // block;
            // j(REL, I, END);
            // Addr instruction;
            String instruction = "after" + i;
            int addr = compiler.address();
            variable.methodAllocations.peek().put(instruction, new Variable.Allocation(4, addr));
            new InstructionStatement("cb", "NEQ", "AI", "LDA", "val", "IL", literal, instruction).compile(compiler, variable, admin, Map.of());
            block.execute(compiler, null);
            new InstructionStatement("j", "REL", "I", "end").compile(compiler, variable, admin, Map.of());

            compiler.address(addr);

            i += 2;
        }
        compiler.address(endAddr);
        compiler.popContext();
        variable.methodAllocations.pop();
    }

}
