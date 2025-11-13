package language.machine;

import language.core.Scope;
import language.core.Structure;

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
    Resolved resolve(String toResolve, Scope variable, Scope scope) {
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
                int literal = scope.findLiteral(toResolve).orElseThrow();
                return new Resolved(4, literal);
            }
            case CL -> {
                int literal = scope.findLiteral(toResolve).orElseThrow();
                return new Resolved(4, literal);
            }
            case LDA -> {
                // Local allocation
                if (scope.findAllocation(toResolve) != null) {
                    Scope.Allocation allocation = scope.findAllocation(toResolve);
                    return new Resolved(allocation.size(), allocation.location());
                }
                if (variable.allocations.containsKey(toResolve)) {
                    Scope.Allocation allocation = variable.allocations.get(toResolve);
                    return new Resolved(allocation.size(), allocation.location());
                }
                return new Resolved(4, -1);
            }
            case ADA -> {
                // argument should be type variable
                String[] split = toResolve.split("\\.");
                Scope v = scope.findVariable(split[0]);
                if (v == null) throw new RuntimeException();
                if (split.length == 2) {
                    // we just want an allocation
                    Scope.Allocation allocation = v.allocations.get(split[1]);
                    return new Resolved(allocation.size(), allocation.location());
                }
                // we want the variable
                int start = Integer.MAX_VALUE;
                int size = 0;
                for (Scope.Allocation a : v.allocations.values()) {
                    if (a.location() < start) start = a.location();
                    size += a.size();
                }
                if (start == Integer.MAX_VALUE) {
                    throw new RuntimeException("variable has no allocations");
                }
                return new Resolved(size, start);
            }
            case LDS -> {
                // local size
                if (scope.findAllocation(toResolve) != null) {
                    return new Resolved(4, scope.findAllocation(toResolve).size());
                }
                return new Resolved(4, variable.allocations.get(toResolve).size());
            }
            case ADS -> {// argument should be type variable
                String[] split = toResolve.split("\\.");
                Scope v = scope.findVariable(split[0]);
                if (v == null) throw new RuntimeException();
                if (split.length == 2) {
                    // we just want an allocation
                    return new Resolved(4, v.allocations.get(split[1]).size());
                }
                // we want the variable
                int size = 0;
                for (Scope.Allocation a : v.allocations.values()) {
                    size += a.size();
                }
                return new Resolved(4, size);
            }
            case LG -> {
                Structure u = variable.generics.get(toResolve).structure;
                return new Resolved(4, u.size(null));
            }
            case AG -> {
                String[] split = toResolve.split("\\.");
                Scope var = scope.findVariable(split[0]);

                Structure u = var.generics.get(toResolve).structure;
                return new Resolved(4, u.size(null));
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
