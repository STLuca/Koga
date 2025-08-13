package language.machine;

import language.core.Argument;
import language.core.Context;
import language.core.Variable;

import java.util.Map;

enum InputType {
    R,     // register
    IL,    // immediate literal,
    AL,    // argument literal,
    CL,    // Context literal,
    LDA,   // local data address,
    LDS,   // local data size,
    ADA,   // argument data address
    ADS,   // argument data size
    LG,    // local generic
    AG,    // argument generic
    ;

    record Resolved(int size, int value) {}
    Resolved resolve(String toResolve, Variable variable, Map<String, Argument> arguments, Context context) {
        switch (this) {
            case R -> {
                int index = switch (toResolve) {
                    case "task" -> 0;
                    case "instruction" -> 1;
                    case "altTask" -> 2;
                    case "altInstruction" -> 3;
                    case "object" -> 4;
                    case "table" -> 5;
                    case "altObject" -> 6;
                    case "altTable" -> 7;
                    default -> throw new RuntimeException("invalid register");
                };
                return new Resolved(4, index);
            }
            case IL -> {
                return new Resolved(4, parseLiteral(toResolve));
            }
            case AL -> {
                // arguments should be type literal
                Argument arg = arguments.get(toResolve);
                if (arg.type != Argument.Type.Literal) throw new RuntimeException();
                return new Resolved(4, arg.val);
            }
            case CL -> {
                Argument arg = context.get(toResolve).orElseThrow();
                if (arg.type != Argument.Type.Literal) throw new RuntimeException();
                return new Resolved(4, arg.val);
            }
            case LDA -> {
                // Local allocation
                if (variable.methodAllocations.peek().containsKey(toResolve)) {
                    Variable.Allocation allocation = variable.methodAllocations.peek().get(toResolve);
                    return new Resolved(allocation.size(), allocation.location());
                }
                if (variable.allocations.containsKey(toResolve)) {
                    Variable.Allocation allocation = variable.allocations.get(toResolve);
                    return new Resolved(allocation.size(), allocation.location());
                }
                return new Resolved(4, -1);
            }
            case ADA -> {
                // argument should be type variable
                String[] split = toResolve.split("\\.");
                Argument arg = arguments.get(split[0]);
                if (arg.type != Argument.Type.Variable) throw new RuntimeException();
                if (split.length == 2) {
                    // we just want an allocation
                    Variable.Allocation allocation = arg.variable.allocations.get(split[1]);
                    return new Resolved(allocation.size(), allocation.location());
                }
                // we want the variable
                int start = Integer.MAX_VALUE;
                int size = 0;
                for (Variable.Allocation a : arg.variable.allocations.values()) {
                    if (a.location() < start) start = a.location();
                    size += a.size();
                }
                return new Resolved(size, start);
            }
            case LDS -> {
                // local size
                if (variable.methodAllocations.peek().containsKey(toResolve)) {
                    return new Resolved(4, variable.methodAllocations.peek().get(toResolve).size());
                }
                return new Resolved(4, variable.allocations.get(toResolve).size());
            }
            case ADS -> {// argument should be type variable
                String[] split = toResolve.split("\\.");
                Argument arg = arguments.get(split[0]);
                if (arg.type != Argument.Type.Variable) throw new RuntimeException();
                if (split.length == 2) {
                    // we just want an allocation
                    return new Resolved(4, arg.variable.allocations.get(split[1]).size());
                }
                // we want the variable
                int size = 0;
                for (Variable.Allocation a : arg.variable.allocations.values()) {
                    size += a.size();
                }
                return new Resolved(4, size);
            }
            case LG -> {
                if (variable.documents.containsKey(toResolve)) {
                    throw new RuntimeException();
                    // return new Resolved(variable.compilableGenerics2.get(toResolve).name(), 4);
                } else if (variable.generics.containsKey(toResolve)) {
                    return new Resolved(4, variable.generics.get(toResolve).size(null));
                } else {
                    // Not a concrete class
                }
            }
            case AG -> {
                String[] split = toResolve.split("\\.");
                Variable var = arguments.get(split[0]).variable;
                if (var.documents.containsKey(split[1])) {
                    throw new RuntimeException();
                    // return new Resolved(variable.compilableGenerics2.get(toResolve).name(), 4);
                } else if (var.generics.containsKey(split[1])) {
                    return new Resolved(4, variable.generics.get(split[1]).size(null));
                } else {
                    // Not a concrete class
                }
            }
        }
        throw new RuntimeException();
    }

    static int parseLiteral(String v) {
        if (v.startsWith("0d")) {
            return Integer.parseInt(v.substring(2));
        }
        if (v.equals("true")) return 1;
        if (v.equals("false")) return 0;
        if (v.matches("'[a-zA-Z]'")) return v.charAt(1);
        try {
            return Integer.parseInt(v);
        } catch (RuntimeException ignored) {}
        throw new RuntimeException("Not implemented");
    }
}
