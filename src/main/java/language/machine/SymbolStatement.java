package language.machine;

import core.Document;
import language.core.*;

import java.util.ArrayList;
import java.util.Map;

public class SymbolStatement implements Statement {

    ArrayList<String> arguments = new ArrayList<>();

    // Logic here is annoying, if document type then you need to lookup the global document name
    // generics already have the global document name
    public void compile(Compiler.MethodCompiler compiler, Sources sources, Variable variable, Map<String, Argument> arguments, Context context) {
        Document.Symbol.Type type = Document.Symbol.Type.valueOf(this.arguments.getFirst().toUpperCase());
        String[] names = new String[2];
        for (int i = 2; i < this.arguments.size(); i+=2) {
            InputType inputType = InputType.valueOf(this.arguments.get(i).toUpperCase());
            int index = (i / 2) - 1;
            String input = this.arguments.get(i + 1);
            names[index] = switch (inputType) {
                case IL -> input;
                case AL -> {
                    String name = arguments.get(input).name;

                    switch (type) {
                        case CLASS, PROTOCOL, METHOD, FIELD, INTERFACE -> {
                            if (index == 0) {
                                name = sources.document(name, Compilable.Level.Head).name;
                            }
                        }
                    }
                    yield name;
                }
                case LG -> {
                    int gIndex = variable.usable.genericIndex(input);
                    Variable.Generic g = variable.generics.get(gIndex);
                    yield g.document.name;
                }
                case AG -> {
                    String[] split = input.split("\\.");
                    Variable var = arguments.get(split[0]).variable;
                    int gIndex = var.usable.genericIndex(split[1]);
                    Variable.Generic g = var.generics.get(gIndex);
                    yield g.document.name;
                }
                default -> throw new RuntimeException();
            };
        }

        int symbol;
        if (names[1] == null) {
            symbol = compiler.symbol(type, names[0]);
        } else {
            symbol = compiler.symbol(type, names[0], names[1]);
        }

        Argument arg = Argument.of(symbol);
        arguments.put(this.arguments.get(1), arg);
    }

}
