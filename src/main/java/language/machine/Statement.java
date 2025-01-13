package language.machine;

import language.core.Compiler;
import language.core.Argument;
import language.core.Variable;

import java.util.Map;

public interface Statement {

    void compile(Compiler.MethodCompiler compiler, Variable variable, Variable admin, Map<String, Argument> arguments);

}
