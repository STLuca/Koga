package language.host;

import core.Types;
import language.core.*;

import java.util.ArrayList;
import java.util.List;

public class HostCompilable implements Compilable {

    String name;
    ArrayList<String> imports = new ArrayList<>();
    ArrayList<String> dependencies = new ArrayList<>();
    ArrayList<Constant> constants = new ArrayList<>();
    String administrator;
    ArrayList<String> supporting = new ArrayList<>();
    ArrayList<Field> fields = new ArrayList<>();
    ArrayList<Method> methods = new ArrayList<>();

    public String name() {
        return name;
    }

    @Override
    public language.core.Document document() {
        HostDocument doc = new HostDocument();
        doc.host = this;
        return doc;
    }

    public List<String> dependencies() {
        ArrayList<String> dependencies = new ArrayList<>();
        for (String dependency : this.dependencies) {
            dependencies.add(dependency);
        }
        return dependencies;
    }

    public void compile(Repository repository, Compiler compiler) {
        compiler.name(name);
        compiler.type(Types.Document.Host);

        for (String imprt : this.imports) {
            repository.structure(imprt);
        }

        String administrator = null;
        for (String dependency : dependencies) {
            Document document = repository.document(dependency);

            compiler.dependency(dependency);
            if (administrator == null) {
                for (String documentImplementing : document.implementing()) {
                   if (documentImplementing.equals("core.Administrator")) {
                       administrator = document.name();
                       break;
                   }
                }
            }
        }
        if (administrator == null) {
            throw new RuntimeException("No administrator dependency");
        }
        compiler.administrator(administrator);

        for (Constant constant : constants) {
            byte[] bytes = new byte[constant.literals.size()];
            int i = 0;
            for (String literal : constant.literals) {
                switch (constant.type) {
                    case String -> bytes[i] = Byte.parseByte(literal);
                    case Nums -> bytes[i] = Byte.parseByte(literal, 10);
                }
                i++;
            }
            compiler.constant(constant.name, bytes);
        }

        for (Field f : fields) {
            Structure sc = repository.structure(f.structure);
            compiler.data(f.name, sc.size(repository));
        }

        for (Method m : methods) {
            Compiler.MethodCompiler mb = compiler.method();
            Scope scope = Scope.withImplicit();
            mb.name(m.name);

//            for (Field f : fields) {
//                scope.putVariable(f.name);
//            }

            // declare the parameters
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

            scope.debugData(mb);
        }
    }

}
