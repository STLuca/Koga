package language.machine;

import language.core.Compiler;
import language.core.Argument;
import language.core.Context;
import language.core.Variable;

import java.util.Map;

public interface Statement {

    void compile(Compiler.MethodCompiler compiler, Variable variable, Map<String, Argument> arguments, Context context);

}
