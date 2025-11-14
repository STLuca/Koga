package language.machine;

import language.core.*;

import java.util.ArrayList;

public class DirectionStatement implements Statement {

    ArrayList<String> arguments = new ArrayList<>();

    public void compile(Compiler.MethodCompiler compiler, Repository repository, Scope variable, Scope scope) {
        Compiler.MethodCompiler.InsertDirection direction = Compiler.MethodCompiler.InsertDirection.valueOf(this.arguments.get(0).toUpperCase());
        compiler.direction(direction);
    }

}
