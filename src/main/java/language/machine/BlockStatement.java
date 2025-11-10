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
    
    public void compile(Compiler.MethodCompiler compiler, Sources sources, Scope variable, Map<String, Argument> arguments, Scope scope) {
        if (isContextPush) {
            scope.add(name, Argument.of(new MachineBlock(this.block, sources, variable, arguments, scope, scope.state())));
            return;
        }
        Argument obj = null;
        if (arguments.containsKey(this.name)) { obj = arguments.get(this.name); }
        else if (scope.get(name).isPresent()) { obj = scope.get(name).get(); }
        if (obj == null) {
            if (block == null) throw new RuntimeException("Block doesn't exist");
            for (Statement statement : block) {
                statement.compile(compiler, sources, variable, arguments, scope);
            }
            return;
        }
        Block bm = obj.block;
        bm.execute(compiler, scope);
    }

    static class MachineBlock implements language.core.Block {

        List<Statement> statements;
        Sources sources;
        Scope variable;
        Map<String, Argument> arguments;
        Scope scope;
        Scope state;

        public MachineBlock(List<Statement> statements, Sources sources, Scope variable, Map<String, Argument> arguments, Scope scope, Scope state) {
            this.statements = statements;
            this.sources = sources;
            this.variable = variable;
            this.arguments = arguments;
            this.scope = scope;
            this.state = state;
        }
        
        public void execute(Compiler.MethodCompiler compiler, Scope scope) {
            for (Statement s : statements) {
                s.compile(compiler, sources, variable, arguments, this.state);
            }
        }
    }

}
