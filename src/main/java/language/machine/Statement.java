package language.machine;

import language.core.*;

public interface Statement {

    void compile(Compiler.MethodCompiler compiler, Sources sources, Scope variable, Scope scope);

}
