package language.core;

public class Argument {

    public enum Type { Literal, Name, Variable, Block }
    public Type type;
    public int val;
    public String name;
    public Block block;
    public Scope variable;

    public static Argument of(int val) {
        Argument arg = new Argument();
        arg.type = Type.Literal;
        arg.val = val;
        return arg;
    }

    public static Argument of(String name) {
        Argument arg = new Argument();
        arg.type = Type.Name;
        arg.name = name;
        return arg;
    }

    public static Argument of(Scope variable) {
        Argument arg = new Argument();
        arg.type = Type.Variable;
        arg.variable = variable;
        return arg;
    }

    public static Argument of(Block block) {
        Argument arg = new Argument();
        arg.type = Type.Block;
        arg.block = block;
        return arg;
    }
}
