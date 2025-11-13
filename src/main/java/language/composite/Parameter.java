package language.composite;

import java.util.ArrayList;

public class Parameter {

    public enum Type { Literal, Name, Variable, Block }

    Type type;
    String name;
    // Type literal
    int bits;
    // Type variable
    String structure;
    ArrayList<String> generics = new ArrayList<>();
    int generic = -1;

}
