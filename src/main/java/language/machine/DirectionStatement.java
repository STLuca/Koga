package language.machine;

import language.core.*;

import java.util.ArrayList;
import java.util.Map;

public class DirectionStatement implements Statement {

    ArrayList<String> arguments = new ArrayList<>();

    public void compile(Compiler.MethodCompiler compiler, Sources sources, Scope variable, Map<String, Argument> arguments, Scope scope) {
        Compiler.MethodCompiler.InsertDirection direction = Compiler.MethodCompiler.InsertDirection.valueOf(this.arguments.get(0).toUpperCase());
        compiler.direction(direction);
    }

}
