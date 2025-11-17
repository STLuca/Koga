package language.composite;

import language.core.*;

public class BlockStatement implements Statement {

    String blockName;

    @Override
    public void handle(Compiler.MethodCompiler compiler, Repository repository, Scope scope) {
        Scope.Block b = scope.findBlock(blockName).orElseThrow();
        b.execute(compiler, scope);
    }
}
