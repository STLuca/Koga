package language.enumeration;

import java.util.ArrayList;

public class Method {

    String name;
    ArrayList<Parameter> params = new ArrayList<>();
    ArrayList<Statement> statements = new ArrayList<>();

    void addParam(String typeName, String name) {
        Parameter p = new Parameter();
        p.name = name;
        if (typeName.equals("Block")) {
            p.type = Parameter.Type.Block;
        } else if (typeName.equals("Name")) {
            p.type = Parameter.Type.Name;
        } else {
            p.type = Parameter.Type.Variable;
            p.descriptor = new Descriptor();
            p.descriptor.name = typeName;
            p.descriptor.type = Descriptor.Type.Structure;
        }
        params.add(p);
    }

    void addParam(int bits, String name) {
        Parameter p = new Parameter();
        p.type = Parameter.Type.Literal;
        p.bits = bits;
        p.name = name;
        params.add(p);
    }

}
