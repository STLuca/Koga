package language.machine;

import language.core.Compiler;
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
                Scope.Generic generic = variable.findGeneric(name).orElse(null);
                if (generic == null) return false;
                Scope v = operation.findVariable(arg).orElseThrow();
                matches = generic.structure == v.structure();
            } else {
                Scope v = operation.findVariable(arg).orElse(null);
                if (v == null) {
                    return false;
                }
                matches = name.equals(v.structure().name());
            }
            if (!matches) {
                return false;
            }
            if (subMatchers == null) {
                return true;
            }
            Scope v = operation.findVariable(arg).orElseThrow();
            if (subMatchers.size() != v.genericSize()) {
                return false;
            }
            boolean allMatch = true;
            List<Scope.Generic> orderedGenerics = v.generics();
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
    ArrayList<String> addresses = new ArrayList<>();
    ArrayList<Statement> body = new ArrayList<>();

    boolean matches(Scope variable, Scope operation, String name, List<String> arguments) {
        if (!name.equals(this.name)) return false;
        List<Scope> defaults = operation.defaults();
        int argumentSize = arguments.size() + defaults.size();
        if (parameters.size() != argumentSize) return false;
        for (int i = 0; i < arguments.size(); i++) {
            Parameter param = parameters.get(i);
            String arg = arguments.get(i);
            if (param.array) {
                operation.findLiteralAsInt(arg);
            }
            switch (param.type) {
                case Variable -> {
                    if (param.variableMatcher.match(variable, operation, arg)) { continue; }
                }
                case Literal -> {
                    Integer literal = operation.findLiteralAsInt(arg).orElse(null);
                    if (literal != null) { continue; }
                }
                case Block -> {
                    Scope.Block b = operation.findBlock(arg).orElse(null);
                    if (b != null) { continue; }
                }
                case Name -> {
                    continue;
                }
            }
            return false;
        }
        for (int i = 0; i < defaults.size(); i++) {
            Parameter param = parameters.get(i + arguments.size());
            Scope arg = defaults.get(i);
            if (param.type != Parameter.Type.Variable || !param.variableMatcher.name.equals(arg.structure().name())) {
                return false;
            }
        }

        return true;
    }

    void populateScope(Compiler.MethodCompiler compiler, Scope scope, Scope operationScope, List<String> arguments) {
        for (int i = 0; i < arguments.size(); i++) {
            Parameter p = parameters.get(i);
            String arg = arguments.get(i);
            switch(p.type) {
                case Literal -> {
                    byte[] literal = scope.findLiteral(arg).orElseThrow();
                    operationScope.put(p.name, literal);
                }
                case Variable -> {
                    Scope v = scope.findVariable(arg).orElseThrow();
                    operationScope.put(p.name, v);
                }
                case Block -> {
                    Scope.Block b = scope.findBlock(arg).orElseThrow();
                    operationScope.put(p.name, b);
                }
                case Name -> {
                    operationScope.put(p.name, arg);
                }
            }
        }
        List<Scope> defaults = scope.defaults();
        for (int i = 0; i < defaults.size(); i++) {
            Parameter p = parameters.get(i + arguments.size());
            Scope arg = defaults.get(i);
            operationScope.put(p.name, arg);
        }
        for (String address : addresses) {
            Integer addressId = operationScope.findAddress(address).orElse(null);
            if (addressId == null) {
                operationScope.putAddress(address, compiler.address());
            }
        }
    }

}
