package language.machine;

import language.core.*;

import java.util.ArrayList;

public class ContextStatement implements Statement {

    enum ContextType {
        PUSH,
        POP,
        VALUE,
        IMPLICIT,
        REMOVE
    }
    
    ArrayList<String> arguments = new ArrayList<>();
    
    public void compile(Compiler.MethodCompiler compiler, Repository repository, Scope variable, Scope scope) {
        ContextType type = ContextType.valueOf(this.arguments.get(0));
        switch (type) {
            case PUSH -> {
                scope.addDefault(variable.name);
            }
            case POP -> {
                scope.remove();
            }
            case VALUE -> {
                String name = this.arguments.get(1);
                InputType inType = InputType.valueOf(this.arguments.get(2));
                String resolveName = this.arguments.get(3);
                InputType.Resolved r = inType.resolve(resolveName, variable, scope);
                scope.literals.put(name, r.value());
            }
            case IMPLICIT -> {
                String name = this.arguments.get(1);
                InputType inType = InputType.valueOf(this.arguments.get(2));
                String resolveName = this.arguments.get(3);
                InputType.Resolved r = inType.resolve(resolveName, variable, scope);
                scope.implicitScope.literals.put(name, r.value());
            }
            case REMOVE -> {

            }
        }
    }
}
