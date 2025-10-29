package language.enumeration;

import language.core.Argument;

import java.util.ArrayList;

public class Method {

    String name;
    ArrayList<Parameter> params = new ArrayList<>();
    ArrayList<Statement> statements = new ArrayList<>();

    void addParam(String typeName, String name) {
        Parameter p = new Parameter();
        p.name = name;
        if (typeName.equals("Block")) {
            p.type = Argument.Type.Block;
        } else if (typeName.equals("Name")) {
            p.type = Argument.Type.Name;
        } else {
            p.type = Argument.Type.Variable;
            p.structure = typeName;
        }
        params.add(p);
    }

    void addParam(int bits, String name) {
        Parameter p = new Parameter();
        p.type = Argument.Type.Literal;
        p.bits = bits;
        p.name = name;
        params.add(p);
    }

}
