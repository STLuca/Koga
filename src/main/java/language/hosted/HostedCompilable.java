package language.hosted;

import core.Types;
import language.core.*;
import language.core.Compiler;

import java.util.ArrayList;
import java.util.List;

public class HostedCompilable implements Compilable {

    String name;
    ArrayList<String> imports = new ArrayList<>();
    ArrayList<String> dependencies = new ArrayList<>();
    ArrayList<Generic> generics = new ArrayList<>();
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

    @Override
    public language.core.Document document() {
        HostedDocument doc = new HostedDocument();
        doc.host = this;
        return doc;
    }

    public void compile(Repository repository, Compiler compiler, Level level) {
        compiler.name(name);
        compiler.type(Types.Document.Hosted);

        for (String imprt : this.imports) {
            repository.structure(imprt);
        }

        if (level == Level.Head) {
            for (Field f : fields) {
                Structure sc = repository.structure(f.structure);
                compiler.data(f.name, sc.size(repository));
            }

            for (Method m : methods) {
                Compiler.MethodCompiler mb = compiler.method();
                mb.name(m.name);
                for (Parameter p : m.params) {
                    Structure structure = repository.structure(p.structure);
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
            compiler.dependency(dependency);
        }
        for (String intrface : interfaces) {
            compiler.implementing(intrface);
        }

        for (Field f : fields) {
            Structure sc = repository.structure(f.structure);
            compiler.data(f.name, sc.size(repository));
        }

        for (Method m : methods) {
            Compiler.MethodCompiler mb = compiler.method();
            Scope scope = Scope.withImplicit();
            for (Generic g : generics) {
                Scope.Generic generic = new Scope.Generic();
                switch (g.type) {
                    case Structure -> {
                        generic.type = Scope.Generic.Type.Structure;
                    }
                    case null, default -> throw new RuntimeException();
                }
                generic.known = false;
                generic.name = g.name;
                scope.put(g.name, generic);
            }
            mb.name(m.name);

//            for (Field f : fields) {
//                scope.putVariable(f.name);
//            }

            for (Parameter p : m.params) {
                ArrayList<String> generics = new ArrayList<>();
                for (Structure.GenericArgument g : p.generics) {
                    generics.add(g.name);
                }

                Structure structure = repository.structure(p.structure);
                mb.parameter(structure.name());
                structure.declare(mb, repository, scope, p.name, p.generics);
            }

            // handle each statement in the body
            for (Statement stmt : m.statements) {
                stmt.handle(mb, repository, scope);
            }
        }
    }

}
