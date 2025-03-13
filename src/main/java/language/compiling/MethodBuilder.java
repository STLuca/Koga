package language.compiling;

import core.Document;
import core.Instruction;
import language.core.Compiler;

import java.util.*;

class MethodBuilder implements Compiler.MethodCompiler {

    DocumentBuilder documentBuilder;
    String name;
    int currData;
    int currAddress;
    ArrayList<Instructions.Item> addresses = new ArrayList<>();
    InsertDirection direction;
    Instructions.Item position;
    Instructions instructions = new Instructions();
    ArrayList<String> params = new ArrayList<>();
    ArrayList<Document.Data> debugData = new ArrayList<>();

    public MethodBuilder(DocumentBuilder documentBuilder) {
        this.documentBuilder = documentBuilder;
        currData = 0;
        // set up the start address
        position = instructions.start.addressAfter();
        addresses.add(position);
        currAddress = 1;
        direction = InsertDirection.BEFORE;
    }

    Document.Method method() {
        List<InstructionBuilder> instructions = this.instructions.list();

        Document.Method m = new Document.Method();
        m.name = name;
        m.size = currData;
        m.instructions = new Instruction[instructions.size()];
        for (int i = 0; i < instructions.size(); i++) {
            InstructionBuilder ib = instructions.get(i);
            m.instructions[i] = ib.instruction(instructions, addresses);
        }
        m.parameters = params.toArray(new String[0]);

        // DEBUG
        m.data = debugData.toArray(new Document.Data[0]);
        return m;
    }
    
    public void name(String name) {
        this.name = name;
    }

    public void debugData(String variableName, String name, int location, int size) {
        debugData.add(new Document.Data(variableName + "." + name, location, size));
    }

    public int data(int size) {
        int curr = currData;
        currData+=size;
        return curr;
    }

    public int address() {
        Instructions.Item address = null;
        switch (direction) {
            case BEFORE: { address = position.addressBefore(); break; }
            case AFTER: { address = position.addressAfter(); break; }
        }
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
        switch (direction) {
            case BEFORE: { addresses.set(index, position.addressBefore()); break; }
            case AFTER: { addresses.set(index, position.addressAfter()); break; }
        }
    }

    public int position(int addr) {
        int prev = addresses.indexOf(position);
        position = addresses.get(addr);
        return prev;
    }

    public void direction(InsertDirection direction) {
        this.direction = direction;
    }

    public Compiler.InstructionCompiler instruction() {
        InstructionBuilder ic = new InstructionBuilder();
        switch (direction) {
            case BEFORE: { position.insertBefore(ic); break; }
            case AFTER: { position.insertAfter(ic); break; }
        }
        return ic;
    }
    
    public int symbol(Document.Symbol.Type type, String name) {
        return documentBuilder.symbol(type, name);
    }
    
    public int symbol(Document.Symbol.Type type, String document, String name) {
        return documentBuilder.symbol(type, document, name);
    }

    public int constant(byte[] bytes) {
        String name = "c_" + documentBuilder.constants.size();
        DocumentBuilder.Constant c = new DocumentBuilder.Constant(name, bytes);
        documentBuilder.constants.add(c);
        return documentBuilder.symbol(Document.Symbol.Type.CONST, name);
    }

    public void parameter(String name) {
        params.add(name);
    }
}
