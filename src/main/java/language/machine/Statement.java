package language.machine;

import language.core.*;

import java.util.Map;

public interface Statement {

    void compile(Compiler.MethodCompiler compiler, Sources sources, Scope variable, Map<String, Argument> arguments, Scope scope);

}
