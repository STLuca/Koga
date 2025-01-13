package language.machine;

import core.Document;
import language.core.Argument;
import language.core.Compiler;
import language.core.Context;
import language.core.Variable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SymbolStatement implements Statement {

    ArrayList<String> arguments = new ArrayList<>();
    
    public void compile(Compiler.MethodCompiler compiler, Variable variable, Map<String, Argument> arguments, Context context) {
        Document.Symbol.Type type = Document.Symbol.Type.valueOf(this.arguments.getFirst().toUpperCase());
        String[] names = new String[2];
        for (int i = 2; i < this.arguments.size(); i+=2) {
            InputType inputType = InputType.valueOf(this.arguments.get(i).toUpperCase());
            int index = (i / 2) - 1;
            String input = this.arguments.get(i + 1);
            names[index] = switch (inputType) {
                case IL -> input;
                case AL -> arguments.get(input).name;
                case LG -> variable.documents.get(input).name;
                case AG -> {
                    String[] split = input.split("\\.");
                    Variable var = arguments.get(split[0]).variable;
                    yield var.documents.get(split[1]).name;
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
