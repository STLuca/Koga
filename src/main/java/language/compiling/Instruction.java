package language.compiling;

import core.Types;

import java.io.DataOutputStream;
import java.io.IOException;

public class Instruction {

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

    public Types.InputType inputType;

    public int src1Size;
    public int src1;
    public int src2Size;
    public int src2;
    public int src3Size;
    public int src3;

    void write(DataOutputStream b) throws IOException {
        b.writeInt(type.ordinal());
        switch (type) {
            case Integer -> b.writeInt(lType.ordinal());
            case Jump -> b.writeInt(jType.ordinal());
            case ConditionalBranch -> b.writeInt(bType.ordinal());
            case Class -> b.writeInt(cmType.ordinal());
            case Memory -> b.writeInt(mType.ordinal());
            case Logician -> b.writeInt(lgType.ordinal());
            case Math -> b.writeInt(mathType.ordinal());
            case Float -> b.writeInt(fType.ordinal());
            case Atomic -> b.writeInt(aType.ordinal());
            case Vector -> b.writeInt(vType.ordinal());
            case Debug -> b.writeInt(dType.ordinal());
        }
        b.writeInt(inputType.ordinal());
        b.writeInt(src1Size);
        b.writeInt(src1);
        b.writeInt(src2Size);
        b.writeInt(src2);
        b.writeInt(src3Size);
        b.writeInt(src3);
    }
}
