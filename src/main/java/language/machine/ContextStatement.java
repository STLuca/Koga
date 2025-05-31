package language.machine;

import language.core.*;

import java.util.ArrayList;
import java.util.Map;

public class ContextStatement implements Statement {

    enum ContextType {
        PUSH,
        POP,
        VALUE,
        IMPLICIT,
        REMOVE
    }
    
    ArrayList<String> arguments = new ArrayList<>();
    
    public void compile(Compiler.MethodCompiler compiler, Sources sources, Variable variable, Map<String, Argument> arguments, Context context) {
        ContextType type = ContextType.valueOf(this.arguments.get(0));
        switch (type) {
            case PUSH -> context.add(Argument.of(variable));
            case POP -> context.remove();
            case VALUE -> {
                String name = this.arguments.get(1);
                InputType inType = InputType.valueOf(this.arguments.get(2));
                String resolveName = this.arguments.get(3);
                InputType.Resolved r = inType.resolve(resolveName, variable, arguments, context);
                arguments.put(name, Argument.of(r.value()));
            }
            case IMPLICIT -> {
                String name = this.arguments.get(1);
                InputType inType = InputType.valueOf(this.arguments.get(2));
                String resolveName = this.arguments.get(3);
                InputType.Resolved r = inType.resolve(resolveName, variable, arguments, context);
                context.add(name, Argument.of(r.value()));
            }
            case REMOVE -> {
                String name = this.arguments.get(1);
                context.remove(name);
            }
        }
    }
}
