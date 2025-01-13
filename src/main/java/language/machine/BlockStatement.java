package language.machine;

import language.core.*;
import language.core.Compiler;

import java.util.Map;

public class BlockStatement implements Statement {

    String name;

    
    public void compile(Compiler.MethodCompiler compiler, Variable variable, Variable admin, Map<String, Argument> arguments) {
        if (!arguments.containsKey(this.name)) throw new RuntimeException("Block doesn't exist");
        Argument obj = arguments.get(this.name);
        Block bm = obj.block;
        bm.execute(compiler, null);
    }

}
