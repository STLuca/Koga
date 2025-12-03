package language.enumeration;

import language.core.*;

import java.util.ArrayList;
import java.util.List;

class Structure implements language.core.Structure {

    String name;
    ArrayList<Field> fields = new ArrayList<>();
    ArrayList<Method> constructors = new ArrayList<>();
    ArrayList<Method> methods = new ArrayList<>();

    public int size(Repository repository) {
        int size = 0;
        for (Field f : fields) {
            language.core.Structure u = repository.structure(f.descriptor.name).orElseThrow();
            size += u.size(repository);
        }
        return size;
    }

    @Override
    public String name() {
        return "";
    }

    @Override
    public void declare(Compiler.MethodCompiler compiler, Repository repository, Scope scope, Scope.Description descriptor, String name) {

    }

    @Override
    public void construct(Compiler.MethodCompiler compiler, Repository repository, Scope scope, Scope.Description descriptor, String name, String constructorName, List<String> argumentNames) {

    }

    @Override
    public void operate(Compiler.MethodCompiler compiler, Repository repository, Scope scope, Scope variable, String operationName, List<String> arguments) {
        Method method = null;
        for (Method m : methods) {
            if (m.name.equals(operationName)) {
                method = m;
                break;
            }
        }
        if (method == null) throw new RuntimeException("Method not found");

        Scope operationScope = scope.startOperation(variable, operationName);
        // Map the args to name using parameters
        int i = 0;
        for (Parameter param : method.params) {
            String arg = arguments.get(i++);
            switch (param.type) {
                case Literal -> {
                    int literal = scope.findLiteralAsInt(arg).orElseThrow();
                    operationScope.put(param.name, literal);
                }
                case Variable -> {
                    Scope v = scope.findVariable(arg).orElseThrow();
                    operationScope.put(param.name, v);
                }
                case Block -> {
                    Scope.Block b = scope.findBlock(arg).orElseThrow();
                    operationScope.put(param.name, b);
                }
                case Name -> {
                    operationScope.put(param.name, arg);
                }
            }
        }

        for (Statement stmt : method.statements) {
            stmt.handle(compiler, repository, operationScope);
        }
    }
}
