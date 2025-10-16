package language.compiling;

import core.Instruction;
import core.Types;
import language.core.Compiler;

import java.util.List;

public class InstructionBuilder implements Compiler.InstructionCompiler {

    static int SRC_COUNT = 3;

    public Types.Instruction type;

    public Types.IntegerType lType;
    public Types.BranchType jType;
    public Types.ConditionalBranchType bType;
    public Types.ClassType cmType;
    public Types.MemoryType mType;
    public Types.LogicianType lgType;
    public Types.InterruptType iType;
    public Types.DebugType dType;
    public Types.MathType mathType;
    public Types.FloatType fType;
    public Types.AtomicType aType;
    public Types.VectorType vType;

    public Types.InputType inputType = Types.InputType.NONE;

    int srcIndex = 0;
    int[] srcs = new int[SRC_COUNT * 2];
    boolean[] addressMarkers = new boolean[SRC_COUNT];

    Instruction instruction(List<InstructionBuilder> instructions, List<Instructions.Item> addressList) {
        for (int i = 0; i < SRC_COUNT; i++) {
            if (!addressMarkers[i]) continue;
            int addr = instructions.indexOf(addressList.get(srcs[i * 2]).prevIb().orElseThrow()) + 1;
            srcs[i * 2] = addr;
        }

        Instruction in = new Instruction();
        in.type = type;
        in.inputType = inputType;

        in.lType = lType;
        in.jType = jType;
        in.bType = bType;
        in.cmType = cmType;
        in.mType = mType;
        in.lgType = lgType;
        in.iType = iType;
        in.dType = dType;
        in.mathType = mathType;
        in.fType = fType;
        in.aType = aType;
        in.vType = vType;

        in.src1     = srcs[0];
        in.src1Size = srcs[1];
        in.src2     = srcs[2];
        in.src2Size = srcs[3];
        in.src3     = srcs[4];
        in.src3Size = srcs[5];

        return in;
    }
    
    public void type(Types.Instruction type) {
        this.type = type;
    }
    
    public void subType(Types.IntegerType lType) {
        this.lType = lType;
    }
    
    public void subType(Types.BranchType jType) {
        this.jType = jType;
    }
    
    public void subType(Types.ConditionalBranchType bType) {
        this.bType = bType;
    }
    
    public void subType(Types.ClassType cmType) {
        this.cmType = cmType;
    }
    
    public void subType(Types.MemoryType mType) {
        this.mType = mType;
    }
    
    public void subType(Types.LogicianType lgType) {
        this.lgType = lgType;
    }
    
    public void subType(Types.InterruptType iType) {
        this.iType = iType;
    }
    
    public void subType(Types.DebugType dType) {
        this.dType = dType;
    }

    @Override
    public void subType(Types.MathType subType) {
        this.mathType = subType;
    }

    @Override
    public void subType(Types.AtomicType subType) {

    }

    @Override
    public void subType(Types.VectorType subType) {

    }

    public void inputType(Types.InputType inputType) {
        this.inputType = inputType;
    }
    
    public void src(int location, int size) {
        srcs[srcIndex] = location;
        srcs[srcIndex + 1] = size;
        srcIndex = srcIndex + 2;
    }
    
    public void src(int address) {
        addressMarkers[srcIndex / 2] = true;
        srcs[srcIndex] = address;
        srcs[srcIndex + 1] = 4;
        srcIndex = srcIndex + 2;
    }
}
