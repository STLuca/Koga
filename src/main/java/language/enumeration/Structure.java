package language.enumeration;

import language.core.*;

import java.util.ArrayList;
import java.util.List;

class Structure implements language.core.Structure {

    String name;
    ArrayList<Field> fields = new ArrayList<>();
    ArrayList<Method> constructors = new ArrayList<>();
    ArrayList<Method> methods = new ArrayList<>();

    public int size(Sources sources) {
        int size = 0;
        for (Field f : fields) {
            language.core.Structure u = sources.structure(f.structure);
            size += u.size(sources);
        }
        return size;
    }

    @Override
    public String name() {
        return "";
    }

    @Override
    public void proxy(Sources sources, Scope variable, int location) {

    }

    @Override
    public void declare(Compiler.MethodCompiler compiler, Sources sources, Scope scope, String name, List<String> generics, List<GenericArgument> nestedGenerics) {

    }

    @Override
    public void construct(Compiler.MethodCompiler compiler, Sources sources, Scope scope, String name, List<String> generics, List<GenericArgument> nestedGenerics, String constructorName, List<String> argumentNames) {

    }

    @Override
    public void operate(Compiler.MethodCompiler compiler, Sources sources, Scope scope, Scope variable, String operationName, List<String> arguments) {
        Method method = null;
        for (Method m : methods) {
            if (m.name.equals(operationName)) {
                method = m;
                break;
            }
        }
        if (method == null) throw new RuntimeException("Method not found");

        Scope operationScope = variable.startOperation(operationName);
        operationScope.scopes.putAll(variable.scopes);
        // Map the args to name using parameters
        int i = 0;
        for (Parameter param : method.params) {
            String arg = arguments.get(i++);
            switch (param.type) {
                case Literal -> {
                    int literal = scope.findLiteral(arg).orElseThrow();
                    operationScope.literals.put(param.name, literal);
                }
                case Variable -> {
                    Scope v = scope.findVariable(arg);
                    if (v == null) { throw new RuntimeException(); }
                    operationScope.scopes.put(param.name, v);
                }
                case Block -> {
                    Block b = scope.findBlock(arg).orElseThrow();
                    operationScope.blocks.put(param.name, b);
                }
                case Name -> {
                    operationScope.names.put(param.name, arg);
                }
            }
        }

        for (Statement stmt : method.statements) {
            stmt.handle(compiler, sources, operationScope);
        }
    }
}
