package language.reference;

import core.Class;
import language.core.*;
import language.core.Compiler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReferenceCompilable implements Compilable {

    String name;
    List<String> imports = new ArrayList<>();
    List<String> dependencies = new ArrayList<>();
    List<String> interfaces = new ArrayList<>();
    List<Field> fields = new ArrayList<>();
    List<Method> methods = new ArrayList<>();

    
    public String name() {
        return name;
    }

    
    public List<String> dependencies() {
        return dependencies;
    }

    public void compile(Classes classes, Compiler compiler) {
        compiler.clazz(name);
        compiler.type(Class.Type.Hosted);
        for (String intrface : interfaces) {
            compiler.implementing(intrface);
        }
        compiler.symbol(Class.Symbol.Type.CLASS, name);

        Map<String, Usable> imports = new HashMap<>();
        for (String imprt : this.imports) {
            imports.put(imprt, classes.usable(imprt));
        }

        Map<String, Compilable> dependencies = new HashMap<>();
        dependencies.put(name, this);
        for (String dependency : this.dependencies) {
            compiler.symbol(Class.Symbol.Type.CLASS, dependency);
            dependencies.put(dependency, classes.compilable(dependency));
        }

        for (Field f : fields) {
            Usable sc = imports.get(f.clazz);
            compiler.data(f.name, sc.size());
        }

        for (Method m : methods) {
            Compiler.MethodCompiler mb = compiler.method();
            mb.name(m.name);

            Map<String, Variable> variables = new HashMap<>();

            // empty construct the parameters
            for (Parameter p : m.params) {
                Variable variable = p.variable(imports);
                variables.put(p.name, variable);
                variable.clazz.declare(mb, classes, variable, p.generics);
            }

            // handle each statement in the body
            for (Statement stmt : m.statements) {
                stmt.handle(mb, classes, variables, null);
            }
        }
    }

}
