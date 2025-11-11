package language.hosted;

import core.Document;
import core.Types;
import language.core.*;
import language.core.Compiler;

import java.util.ArrayList;
import java.util.List;

public class HostedCompilable implements Compilable {

    String name;
    ArrayList<String> imports = new ArrayList<>();
    ArrayList<String> dependencies = new ArrayList<>();
    ArrayList<String> interfaces = new ArrayList<>();
    ArrayList<Field> fields = new ArrayList<>();
    ArrayList<Method> methods = new ArrayList<>();
    
    public String name() {
        return name;
    }

    public List<String> dependencies() {
        ArrayList<String> dependencies = new ArrayList<>();
        for (String dependency : this.dependencies) {
            dependencies.add(dependency);
        }
        return dependencies;
    }

    public void compile(Sources sources, Compiler compiler, Level level) {
        compiler.name(name);
        compiler.type(Types.Document.Hosted);

        for (String imprt : this.imports) {
            sources.structure(imprt);
        }

        if (level == Level.Head) {
            for (Field f : fields) {
                Structure sc = sources.structure(f.structure);
                compiler.data(f.name, sc.size(sources));
            }

            for (Method m : methods) {
                Compiler.MethodCompiler mb = compiler.method();
                mb.name(m.name);
                for (Parameter p : m.params) {
                    Structure structure = sources.structure(p.structure);
                    mb.parameter(structure.name());
                }
            }
            for (String dependency : dependencies) {
                compiler.dependency(dependency);
            }

            for (String implementing : interfaces) {
                compiler.implementing(implementing);
            }
            return;
        }

        for (String dependency : dependencies) {
            Document document = sources.document(dependency, Level.Head);
            compiler.dependency(dependency);
        }
        for (String intrface : interfaces) {
            compiler.implementing(intrface);
        }

        for (Field f : fields) {
            Structure sc = sources.structure(f.structure);
            compiler.data(f.name, sc.size(sources));
        }

        for (Method m : methods) {
            Compiler.MethodCompiler mb = compiler.method();
            Scope scope = Scope.reset();
            mb.name(m.name);

            for (Field f : fields) {
                scope.addVariable(f.name);
            }

            // empty construct the parameters
            for (Parameter p : m.params) {
                Structure structure = sources.structure(p.structure);
                mb.parameter(structure.name());
                structure.declare(mb, sources, scope, p.name, p.generics);
            }

            // handle each statement in the body
            for (Statement stmt : m.statements) {
                stmt.handle(mb, sources, scope);
            }
        }
    }

}
