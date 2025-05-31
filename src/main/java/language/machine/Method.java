package language.machine;

import language.core.Argument;
import language.core.Variable;

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

    boolean matches(Variable variable, String name, List<Argument> args) {
        if (!name.equals(this.name)) return false;
        if (args.size() != parameters.size()) return false;
        for (int i = 0; i < args.size(); i++) {
            Parameter param = parameters.get(i);
            Argument arg = args.get(i);
            if (param.array && arg.type == Argument.Type.Literal) continue;
            switch (param.type) {
                case Variable -> {
                    if (arg.type != Argument.Type.Variable) return false;
                    if (param.className.equals("Any")) continue;
                    if (variable.generics.containsKey(param.className) && variable.generics.get(param.className) == arg.variable.usable) continue;
                    if (param.className.equals(arg.variable.usable.name())) continue;
                }
                case Literal, Name, Block -> {
                    if (param.type == arg.type) continue;
                }
            }
            return false;
        }
        return true;
    }

}
