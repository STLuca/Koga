package language.compiling;

import core.Instruction;
import language.core.Compiler;

import java.util.List;

public class InstructionBuilder implements Compiler.InstructionCompiler {

    static int SRC_COUNT = 4;

    public Instruction.Type type;

    public Instruction.LogicType lType;
    public Instruction.BranchType jType;
    public Instruction.ConditionalBranchType bType;
    public Instruction.ClassType cmType;
    public Instruction.MemoryType mType;
    public Instruction.LogicianType lgType;
    public Instruction.InterruptType iType;
    public Instruction.DebugType dType;

    public Instruction.InputType inputType = Instruction.InputType.NONE;

    public boolean destAddrMarker = false;
    public int destSize;
    public int dest;
    int[] srcs = new int[SRC_COUNT * 2];
    int srcIndex = 0;
    boolean[] addressMarkers = new boolean[] { false, false, false, false };

    Instruction instruction(List<InstructionBuilder> instructions, List<Instructions.Item> addressList) {
        if (destAddrMarker) {
            dest = instructions.indexOf(addressList.get(dest).prevIb().orElseThrow()) + 1;
        }

        for (int i = 0; i < SRC_COUNT; i++) {
            if (!addressMarkers[i]) continue;
            int addr = instructions.indexOf(addressList.get(srcs[i * 2]).prevIb().orElseThrow()) + 1;
            srcs[i * 2] = addr;
        }

        Instruction in = new Instruction();
        in.type = type;
        in.lType = lType;
        in.jType = jType;
        in.bType = bType;
        in.cmType = cmType;
        in.mType = mType;
        in.lgType = lgType;
        in.iType = iType;
        in.dType = dType;
        in.inputType = inputType;

        in.destSize = destSize;
        in.dest = dest;
        in.src1 = srcs[0];
        in.src1Size = srcs[1];
        in.src2 = srcs[2];
        in.src2Size = srcs[3];

        return in;
    }

    
    public void type(Instruction.Type type) {
        this.type = type;
    }

    
    public void subType(Instruction.LogicType lType) {
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

    
    public void inputType(Instruction.InputType inputType) {
        this.inputType = inputType;
    }

    
    public void dest(int location, int size) {
        dest = location;
        destSize = size;
    }

    
    public void dest(int address) {
        destAddrMarker = true;
        dest = address;
        destSize = 4;
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
