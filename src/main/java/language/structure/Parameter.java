package language.structure;

import language.core.Argument;
import language.core.Usable;
import language.core.Variable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Parameter {

    Argument.Type type;
    String name;
    // Type literal
    int bits;
    // Type variable
    String usable;
    ArrayList<String> generics = new ArrayList<>();

}
