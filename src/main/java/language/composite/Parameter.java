package language.composite;

import language.core.Argument;

import java.util.ArrayList;

public class Parameter {

    Argument.Type type;
    String name;
    // Type literal
    int bits;
    // Type variable
    String structure;
    ArrayList<String> generics = new ArrayList<>();

}
