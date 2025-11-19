package language.machine;

import core.Types;
import language.core.*;

import java.util.ArrayList;

public class SymbolStatement implements Statement {

    ArrayList<String> arguments = new ArrayList<>();

    // Logic here is annoying, if document type then you need to lookup the global document name
    // generics already have the global document name
    public void compile(Compiler.MethodCompiler compiler, Repository repository, Scope variable, Scope scope) {
        Types.Symbol type = Types.Symbol.valueOf(this.arguments.getFirst().toUpperCase());
        int nameLength = (this.arguments.size() - 2) / 2;
        if (nameLength != 1 && nameLength != 2) {
            throw new RuntimeException();
        }
        String[] names = new String[nameLength];
        for (int i = 2; i < this.arguments.size(); i+=2) {
            InputType inputType = InputType.valueOf(this.arguments.get(i).toUpperCase());
            int index = (i / 2) - 1;
            String input = this.arguments.get(i + 1);
            names[index] = switch (inputType) {
                case IL -> input;
                case AL -> {
                    String name = scope.findName(input).orElse(null);

                    switch (type) {
                        case CLASS, PROTOCOL, METHOD, INTERFACE -> {
                            if (index == 0) {
                                name = repository.document(name).name();
                            }
                        }
                        case FIELD -> {
                            if (index == 0 && nameLength == 2) {
                                name = repository.document(name).name();
                            }
                        }
                    }
                    yield name;
                }
                case LG -> {
                    Scope.Generic g = scope.findGeneric(input).orElseThrow();
                    yield g.document.name();
                }
                case AG -> {
                    String[] split = input.split("\\.");
                    Scope var = scope.findVariable(split[0]).orElseThrow();
                    Scope.Generic g = var.findGeneric(split[1]).orElseThrow();
                    yield g.document.name();
                }
                default -> throw new RuntimeException();
            };
        }

        int symbol;
        if (nameLength == 1) {
            symbol = compiler.symbol(type, names[0]);
        } else {
            symbol = compiler.symbol(type, names[0], names[1]);
        }

        scope.put(this.arguments.get(1), symbol);
    }

}
