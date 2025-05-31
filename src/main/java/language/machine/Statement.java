package language.machine;

import language.core.*;

import java.util.Map;

public interface Statement {

    void compile(Compiler.MethodCompiler compiler, Sources sources, Variable variable, Map<String, Argument> arguments, Context context);

}
