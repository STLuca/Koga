package language.union;

import language.core.Argument;

import java.util.ArrayList;

public class Parameter {

    Argument.Type type;
    String name;
    // Type literal
    int bits;
    // Type variable
    String usable;
    ArrayList<String> generics = new ArrayList<>();

}
