package language.machine;

import language.core.Compiler;
import language.core.Repository;
import language.core.Scope;

import java.util.ArrayList;

public class ConstantStatement implements Statement {

    ArrayList<String> arguments = new ArrayList<>();

    @Override
    public void compile(Compiler.MethodCompiler compiler, Repository repository, Scope variable, Scope scope) {
        String symbolName = arguments.get(0);
        String literalName = arguments.get(1);
        byte[] literal = scope.findLiteral(literalName).orElseThrow();
        int symbol = compiler.constant(literal);
        scope.put(symbolName, symbol);
    }
}
