package language.machine;

import language.core.Argument;

import java.util.ArrayList;
import java.util.List;

public class Method {

    public static class Parameter {
        Argument.Type type;
        int bits;
        String className;
        boolean array;
        String name;
    }

    String name;
    ArrayList<Parameter> parameters = new ArrayList<>();
    ArrayList<Statement> body = new ArrayList<>();

    void addParam(String className, String name, boolean isArray) {
        Method.Parameter p = new Method.Parameter();
        if (className.equals("Block")) {
            p.type = Argument.Type.Block;
        } else if (className.equals("Name")) {
            p.type = Argument.Type.Name;
        } else {
            p.type = Argument.Type.Variable;
            p.className = className;
        }
        p.array = isArray;
        p.name = name;
        parameters.add(p);
    }

    void addParam(int bits, String name, boolean isArray) {
        Method.Parameter p = new Method.Parameter();
        p.type = Argument.Type.Literal;
        p.bits = bits;
        p.array = isArray;
        p.name = name;
        parameters.add(p);
    }

    boolean matches(String name, List<Argument> args) {
        if (!name.equals(this.name)) return false;
        if (args.size() != parameters.size()) return false;
        for (int i = 0; i < args.size(); i++) {
            if (parameters.get(i).array && args.get(i).type == Argument.Type.Literal) continue;
            if (args.get(i).type == parameters.get(i).type) continue;
            return false;
        }
        return true;
    }

}
