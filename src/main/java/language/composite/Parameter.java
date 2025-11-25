package language.composite;

public class Parameter {

    public enum Type { Literal, Name, Variable, Block }

    Type type;
    String name;
    // Type variable
    Descriptor descriptor;

}
