package language.machine;

import language.core.*;

public interface Statement {

    void compile(Compiler.MethodCompiler compiler, Repository repository, Scope variable, Scope scope);

}
