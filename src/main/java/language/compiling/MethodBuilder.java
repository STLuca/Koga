package language.compiling;

import core.Class;
import core.Instruction;
import language.core.Compiler;

import java.util.*;

class MethodBuilder implements Compiler.MethodCompiler {

    ClassBuilder classBuilder;
    String name;
    int currData = 0;
    int currAddress = 0;
    List<Instructions.Item> addresses = new ArrayList<>();
    Stack<Instructions.Item> views = new Stack<>();
    Instructions instructions = new Instructions();
    List<Class.Data> debugData = new ArrayList<>();

    public MethodBuilder(ClassBuilder classBuilder) {
        this.classBuilder = classBuilder;
        Instructions.Item defaultAddress = instructions.start.addressAfter();
        views.push(defaultAddress);
    }

    Class.Method method() {
        List<InstructionBuilder> instructions = this.instructions.list();

        Class.Method m = new Class.Method();
        m.name = name;
        m.size = currData;
        for (InstructionBuilder ib : instructions) {
            m.instructions.add(ib.instruction(instructions, addresses));
        }

        // DEBUG
        m.data.addAll(debugData);
        return m;
    }

    
    public void name(String name) {
        this.name = name;
    }

    public void pushContext() {
        views.push(views.peek());
    }

    public void popContext() {
        views.pop();
    }

    public void debugData(String variableName, String name, int location, int size) {
        debugData.add(new Class.Data(variableName + "." + name, location, size));
    }

    public int data(int size) {
        int curr = currData;
        currData+=size;
        return curr;
    }

    public int address() {
        Instructions.Item address = views.peek().addressBefore();
        int index = currAddress;
        addresses.add(address);
        currAddress = currAddress + 1;
        return index;
    }

    public void address(int index) {
        Instructions.Item address = addresses.get(index);
        if (address != null) {
            address.remove();
        }
        addresses.set(index, views.peek().addressBefore());
    }

    public void view(String type, int addr) {
        switch (type) {
            case "LINK" -> {
                views.pop();
                views.push(addresses.get(addr));
            }
            case "UNLINK" -> {
                views.pop();
            }
            default -> {
                throw new RuntimeException();
            }
        }
    }

    
    public Compiler.InstructionCompiler instruction() {
        InstructionBuilder ic = new InstructionBuilder();
        views.peek().insertBefore(ic);
        return ic;
    }

    
    public int symbol(Class.Symbol.Type type, String name) {
        return classBuilder.symbol(type, name);
    }

    
    public int symbol(Class.Symbol.Type type, String clazz, String name) {
        return classBuilder.symbol(type, clazz, name);
    }

    public int constant(byte[] bytes) {
        String name = "c_" + classBuilder.constants.size();
        ClassBuilder.Constant c = new ClassBuilder.Constant(name, bytes);
        classBuilder.constants.add(c);
        return classBuilder.symbol(Class.Symbol.Type.CONST, name);
    }
}
