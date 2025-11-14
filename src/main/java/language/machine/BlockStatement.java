package language.machine;

import language.core.*;
import language.core.Compiler;

import java.util.ArrayList;
import java.util.List;

public class BlockStatement implements Statement {

    String name;
    boolean isContextPush;
    ArrayList<Statement> block = new ArrayList<>();
    
    public void compile(Compiler.MethodCompiler compiler, Repository repository, Scope variable, Scope scope) {
        if (isContextPush) {
            MachineBlock newBlock = new MachineBlock(this.block, repository, variable, scope, scope.state());
            scope.implicitScope.blocks.put(name, newBlock);
            return;
        }

        Block bm = null;
        if (scope.findBlock(this.name).isPresent()) {
            bm = scope.findBlock(this.name).orElseThrow();
        } else if (scope.parent.findBlock(this.name).isPresent()) {
            bm = scope.parent.findBlock(this.name).orElseThrow();
        }
        if (bm == null) {
            if (block == null) throw new RuntimeException("Block doesn't exist");
            for (Statement statement : block) {
                statement.compile(compiler, repository, variable, scope);
            }
            return;
        }
        bm.execute(compiler, scope);
    }

    static class MachineBlock implements language.core.Block {

        List<Statement> statements;
        Repository repository;
        Scope variable;
        Scope scope;
        Scope state;

        public MachineBlock(List<Statement> statements, Repository repository, Scope variable, Scope scope, Scope state) {
            this.statements = statements;
            this.repository = repository;
            this.variable = variable;
            this.scope = scope;
            this.state = state;
        }
        
        public void execute(Compiler.MethodCompiler compiler, Scope scope) {
            this.state.addImplicit(scope.implicitScope);
            for (Statement s : statements) {
                s.compile(compiler, repository, variable, this.state);
            }
            this.state.removeImplicit(scope.implicitScope);
        }
    }

}
