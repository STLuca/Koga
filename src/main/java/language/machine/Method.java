package language.machine;

import language.core.Argument;
import language.core.Variable;

import java.util.ArrayList;
import java.util.List;

public class Method {

    public static class VariableMatcher {

        boolean isGeneric;
        String name;
        ArrayList<VariableMatcher> subMatchers;

        boolean match(Variable variable, Argument arg) {
            boolean matches;
            if (isGeneric) {
                int index = variable.usable.genericIndex(name);
                if (index == -1) return false;
                matches = variable.generics.get(index).usable == arg.variable.usable;
            } else {
                matches = name.equals(arg.variable.usable.name());
            }
            if (!matches) {
                return false;
            }
            if (subMatchers == null) {
                return true;
            }
            if (subMatchers.size() != arg.variable.generics.size()) {
                return false;
            }
            boolean allMatch = true;
            for (int i = 0; i < subMatchers.size(); i++) {
                VariableMatcher m = subMatchers.get(i);
                Variable.Generic g = arg.variable.generics.get(i);
                if (!m.name.equals(g.usable.name())) {
                    allMatch = false;
                }
            }
            return allMatch;
        }
    }

    public static class Parameter {
        Argument.Type type;
        int bits;
        VariableMatcher variableMatcher;
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
                    if (param.variableMatcher.match(variable, arg)) continue;
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
