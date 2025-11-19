package language.machine;

import language.core.*;

public class PositionStatement implements Statement {

    String addr;
    String prevName;

    public void compile(Compiler.MethodCompiler compiler, Repository repository, Scope variable, Scope scope) {
        int addr = scope.findAddress(this.addr).orElseThrow();
        int prev = compiler.position(addr);
        if (this.prevName != null) {
            scope.putAddress(prevName, prev);
        }
    }

}
