package language.machine;

import language.core.*;
import language.core.Compiler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BlockStatement implements Statement {

    String name;
    List<Statement> elseBlock = new ArrayList<>();
    
    public void compile(Compiler.MethodCompiler compiler, Variable variable, Map<String, Argument> arguments, Context context) {
        Argument obj = null;
        if (arguments.containsKey(this.name)) { obj = arguments.get(this.name); }
        else if (context.get(name).isPresent()) { obj = context.get(name).get(); }
        if (obj == null) {
            if (elseBlock == null) throw new RuntimeException("Block doesn't exist");
            for (Statement statement : elseBlock) {
                statement.compile(compiler, variable, arguments, context);
            }
            return;
        }
        Block bm = obj.block;
        bm.execute(compiler);
    }

}
