package language.compiling;

import core.Instruction;
import language.core.Compiler;

import java.util.List;

public class InstructionBuilder implements Compiler.InstructionCompiler {

    static int SRC_COUNT = 3;

    public Instruction.Type type;

    public Instruction.IntegerType lType;
    public Instruction.BranchType jType;
    public Instruction.ConditionalBranchType bType;
    public Instruction.ClassType cmType;
    public Instruction.MemoryType mType;
    public Instruction.LogicianType lgType;
    public Instruction.InterruptType iType;
    public Instruction.DebugType dType;
    public Instruction.MathType mathType;
    public Instruction.FloatType fType;
    public Instruction.AtomicType aType;
    public Instruction.VectorType vType;

    public Instruction.InputType inputType = Instruction.InputType.NONE;

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
    
    public void type(Instruction.Type type) {
        this.type = type;
    }
    
    public void subType(Instruction.IntegerType lType) {
        this.lType = lType;
    }
    
    public void subType(Instruction.BranchType jType) {
        this.jType = jType;
    }
    
    public void subType(Instruction.ConditionalBranchType bType) {
        this.bType = bType;
    }
    
    public void subType(Instruction.ClassType cmType) {
        this.cmType = cmType;
    }
    
    public void subType(Instruction.MemoryType mType) {
        this.mType = mType;
    }
    
    public void subType(Instruction.LogicianType lgType) {
        this.lgType = lgType;
    }
    
    public void subType(Instruction.InterruptType iType) {
        this.iType = iType;
    }
    
    public void subType(Instruction.DebugType dType) {
        this.dType = dType;
    }

    @Override
    public void subType(Instruction.MathType subType) {
        this.mathType = subType;
    }

    @Override
    public void subType(Instruction.AtomicType subType) {

    }

    @Override
    public void subType(Instruction.VectorType subType) {

    }

    public void inputType(Instruction.InputType inputType) {
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
