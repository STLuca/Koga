package language.machine;

import language.core.Scope;
import language.core.Structure;

enum InputType {
    R,   // register
    I,   // immediate
    L,   // literal
    P,   // position
    S,   // size
    G,   // generic
    ;

    record Resolved(int size, int value) {}
    Resolved resolve(String toResolve, Scope scope) {
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
            case I -> {
                return new Resolved(4, parseLiteral(toResolve));
            }
            case L -> {
                int literal = scope.findLiteralAsInt(toResolve).orElseThrow();
                return new Resolved(4, literal);
            }
            case P -> {
                String[] split = toResolve.split("\\.");
                Scope curr = scope;
                for (int i = 0; i < split.length - 1; i++) {
                    curr = scope.findVariable(split[i]).orElseThrow();
                }
                Scope.Allocation allocation = curr.findAllocation(split[split.length - 1]).orElse(null);
                if (allocation != null) {
                    return new Resolved(allocation.size(), allocation.location());
                }
                curr = curr.findVariable(split[split.length - 1]).orElse(null);
                if (curr != null) {
                    allocation = curr.allocation().orElseThrow();
                    return new Resolved(allocation.size(), allocation.location());
                }
                return new Resolved(4, -1);
            }
            case S -> {
                String[] split = toResolve.split("\\.");
                Scope curr = scope;
                for (int i = 0; i < split.length - 1; i++) {
                    curr = scope.findVariable(split[i]).orElseThrow();
                }
                Scope.Allocation allocation = curr.findAllocation(split[split.length - 1]).orElse(null);
                if (allocation != null) {
                    return new Resolved(4, allocation.size());
                }
                curr = curr.findVariable(split[split.length - 1]).orElseThrow();
                allocation = curr.allocation().orElseThrow();
                return new Resolved(4, allocation.size());
            }
            case G -> {
                String[] split = toResolve.split("\\.");
                Scope curr = scope;
                for (int i = 0; i < split.length - 1; i++) {
                    curr = scope.findVariable(split[i]).orElseThrow();
                }
                Structure u = curr.findGeneric(split[split.length - 1]).orElseThrow().structure;
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
