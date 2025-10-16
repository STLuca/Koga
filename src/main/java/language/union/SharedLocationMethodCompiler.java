package language.union;

import core.Types;
import language.core.Compiler;

public class SharedLocationMethodCompiler implements Compiler.MethodCompiler {

    Compiler.MethodCompiler parent;
    int location;

    @Override
    public void name(String name) {
        parent.name(name);
    }

    @Override
    public int data(int size) {
        int dataLocation = location;
        location += size;
        return dataLocation;
    }

    @Override
    public int address() {
        return parent.address();
    }

    @Override
    public void address(int index) {
        parent.address(index);
    }

    @Override
    public int position(int addr) {
        return parent.position(addr);
    }

    @Override
    public void direction(InsertDirection direction) {
        parent.direction(direction);
    }

    @Override
    public int symbol(Types.Symbol type, String name) {
        return parent.symbol(type, name);
    }

    @Override
    public int symbol(Types.Symbol type, String document, String name) {
        return parent.symbol(type, document, name);
    }

    @Override
    public int constant(byte[] bytes) {
        return parent.constant(bytes);
    }

    @Override
    public void parameter(String name) {
        parent.parameter(name);
    }

    @Override
    public Compiler.InstructionCompiler instruction() {
        return parent.instruction();
    }

    @Override
    public void debugData(String variableName, String name, int location, int size) {
        parent.debugData(variableName, name, location, size);
    }
}
