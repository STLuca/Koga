package language.composite;

import language.core.Argument;
import language.core.Compiler;
import language.core.Scope;
import language.core.Sources;

import java.util.Map;

public class BlockStatement implements Statement {

    String blockName;

    @Override
    public void handle(Compiler.MethodCompiler compiler, Sources sources, Map<String, Argument> argsByName, Map<String, Scope.Generic> genericsByName, String name, Scope scope) {
        language.core.Argument arg = argsByName.get(blockName);
        if (arg.type != language.core.Argument.Type.Block) {
            throw new RuntimeException();
        }
        arg.block.execute(compiler, scope);
    }
}
