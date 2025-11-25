package language.enumeration;

public class Parameter {

    public enum Type { Literal, Name, Variable, Block }

    Type type;
    String name;
    // Type literal
    int bits;
    // Type variable
    Descriptor descriptor;

}
