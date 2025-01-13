package language.system;

import language.core.Usable;
import language.core.Variable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Parameter {

    String clazz;
    String name;
    List<String> generics = new ArrayList<>();

    Variable variable(Map<String, Usable> imports) {
        Variable variable = new Variable();
        variable.name = name;
        variable.clazz = imports.get(clazz);
        return variable;
    }

}
