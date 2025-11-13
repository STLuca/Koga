package language.machine;

import language.core.Block;
import language.core.Scope;

import java.util.ArrayList;
import java.util.List;

public class Operation {

    public static class VariableMatcher {

        boolean isGeneric;
        String name;
        ArrayList<VariableMatcher> subMatchers;

        boolean match(Scope variable, Scope operation, String arg) {
            boolean matches;
            if (isGeneric) {
                if (!variable.generics.containsKey(name)) return false;
                Scope v = operation.findVariable(arg);
                matches = variable.generics.get(name).structure == v.structure;
            } else {
                Scope v = operation.findVariable(arg);
                if (v == null) {
                    return false;
                }
                matches = name.equals(v.structure.name());
            }
            if (!matches) {
                return false;
            }
            if (subMatchers == null) {
                return true;
            }
            Scope v = operation.findVariable(arg);
            if (subMatchers.size() != v.generics.size()) {
                return false;
            }
            boolean allMatch = true;
            Scope var = operation.findVariable(arg);
            List<Scope.Generic> orderedGenerics = var.generics.sequencedValues().stream().toList();
            for (int i = 0; i < subMatchers.size(); i++) {
                VariableMatcher m = subMatchers.get(i);
                Scope.Generic g = orderedGenerics.get(i);
                if (g.known) {
                    if (!m.isGeneric && !m.name.equals(g.structure.name())) {
                        allMatch = false;
                    }
                } else {
                    if (m.isGeneric && !m.name.equals(g.name)) {
                        allMatch = false;
                    }
                }
            }
            return allMatch;
        }
    }

    public static class Parameter {

        public enum Type { Literal, Name, Variable, Block }

        Type type;
        int bits;
        VariableMatcher variableMatcher;
        boolean array;
        String name;
    }

    String name;
    ArrayList<Parameter> parameters = new ArrayList<>();
    ArrayList<Statement> body = new ArrayList<>();

    boolean matches(Scope variable, Scope operation, String name, List<String> arguments) {
        if (!name.equals(this.name)) return false;
        if (arguments.size() != parameters.size()) return false;
        for (int i = 0; i < arguments.size(); i++) {
            Parameter param = parameters.get(i);
            String arg = arguments.get(i);
            if (param.array) {
                operation.findLiteral(arg);
            }
            switch (param.type) {
                case Variable -> {
                    if (param.variableMatcher.match(variable, operation, arg)) { continue; }
                }
                case Literal -> {
                    Integer literal = operation.findLiteral(arg).orElse(null);
                    if (literal != null) { continue; }
                }
                case Block -> {
                    Block b = operation.findBlock(arg).orElse(null);
                    if (b != null) { continue; }
                }
                case Name -> {
                    continue;
                }
            }
            return false;
        }
        return true;
    }

}
