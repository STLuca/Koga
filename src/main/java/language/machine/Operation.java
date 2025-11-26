package language.machine;

import language.core.Compiler;
import language.core.Repository;
import language.core.Scope;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public class Operation {

    public static class Descriptor {

        public enum Type { Structure, Document, Generic }
        Type type;
        String name;
        ArrayList<Descriptor> subDescriptors = new ArrayList<>();

        Scope.Generic resolve(Scope variable, Repository repository) {
            Scope.Generic rootGeneric = new Scope.Generic();
            ArrayDeque<Scope.Generic> dGenerics = new ArrayDeque<>();
            ArrayDeque<Descriptor> descriptors = new ArrayDeque<>();
            dGenerics.push(rootGeneric);
            descriptors.push(this);
            while (!dGenerics.isEmpty()) {
                Scope.Generic g = dGenerics.pop();
                Descriptor d = descriptors.pop();
                switch (d.type) {
                    case Structure -> {
                        g.type = Scope.Generic.Type.Structure;
                        g.structure = repository.structure(d.name);
                    }
                    case Document -> {
                        g.type = Scope.Generic.Type.Document;
                        g.document = repository.document(d.name);
                    }
                    case Generic -> {
                        g.type = Scope.Generic.Type.Structure;
                        g.structure = variable.findGeneric(d.name).orElseThrow().structure;
                    }
                }
                for (Descriptor subDescriptor : d.subDescriptors) {
                    descriptors.push(subDescriptor);
                    Scope.Generic subGeneric = new Scope.Generic();
                    g.generics.add(subGeneric);
                    dGenerics.push(subGeneric);
                }
            }
            return rootGeneric;
        }

    }

    public static class Parameter {

        public enum Type { Literal, Name, Variable, Block }

        Type type;
        int bits;
        Descriptor descriptor;
        boolean array;
        String name;
    }

    String name;
    ArrayList<Parameter> parameters = new ArrayList<>();
    ArrayList<String> addresses = new ArrayList<>();
    ArrayList<Statement> body = new ArrayList<>();

    boolean matches(Scope variable, Scope operation, String name, List<String> arguments, Repository repository) {
        if (!name.equals(this.name)) return false;
        List<Scope> defaults = operation.defaults();
        int argumentSize = arguments.size() + defaults.size();
        if (parameters.size() != argumentSize) return false;

        for (int i = 0; i < arguments.size(); i++) {
            Parameter param = parameters.get(i);
            String arg = arguments.get(i);
            switch (param.type) {
                case Variable -> {
                    Scope.Generic rootGeneric = param.descriptor.resolve(variable, repository);
                    Scope argument = operation.findVariable(arg).orElse(null);
                    if (argument != null) {
                        Scope.Generic argDescription = argument.description();
                        if (rootGeneric.equals(argDescription)) {
                            continue;
                        }
                    }
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
            if (param.type != Parameter.Type.Variable) {
                return false;
            }
            Scope.Generic rootGeneric = param.descriptor.resolve(variable, repository);
            if (!rootGeneric.equals(arg.description())) {
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
