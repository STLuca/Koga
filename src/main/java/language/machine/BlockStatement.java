package language.machine;

import language.core.*;
import language.core.Compiler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BlockStatement implements Statement {

    String name;
    boolean isContextPush;
    ArrayList<Statement> block = new ArrayList<>();
    
    public void compile(Compiler.MethodCompiler compiler, Sources sources, Context.Scope variable, Map<String, Argument> arguments, Context context) {
        if (isContextPush) {
            context.add(name, Argument.of(new MachineBlock(this.block, sources, variable, arguments, context, context.state())));
            return;
        }
        Argument obj = null;
        if (arguments.containsKey(this.name)) { obj = arguments.get(this.name); }
        else if (context.get(name).isPresent()) { obj = context.get(name).get(); }
        if (obj == null) {
            if (block == null) throw new RuntimeException("Block doesn't exist");
            for (Statement statement : block) {
                statement.compile(compiler, sources, variable, arguments, context);
            }
            return;
        }
        Block bm = obj.block;
        bm.execute(compiler);
    }

    static class MachineBlock implements language.core.Block {

        List<Statement> statements;
        Sources sources;
        Context.Scope variable;
        Map<String, Argument> arguments;
        Context context;
        Context.Scope state;

        public MachineBlock(List<Statement> statements, Sources sources, Context.Scope variable, Map<String, Argument> arguments, Context context, Context.Scope state) {
            this.statements = statements;
            this.sources = sources;
            this.variable = variable;
            this.arguments = arguments;
            this.context = context;
            this.state = state;
        }
        
        public void execute(Compiler.MethodCompiler compiler) {
            Context.Scope current = context.state();
            context.setState(this.state);
            for (Statement s : statements) {
                s.compile(compiler, sources, variable, arguments, context);
            }
            context.setState(current);
        }
    }

}
