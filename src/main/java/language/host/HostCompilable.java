package language.host;

import core.Document;
import language.core.*;
import language.core.Compiler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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

    public List<String> dependencies() {
        ArrayList<String> dependencies = new ArrayList<>();
        for (String dependency : this.dependencies) {
            dependencies.add(dependency);
        }
        return dependencies;
    }

    public void compile(Sources sources, Compiler compiler, Level level) {
        compiler.name(name);
        compiler.type(Document.Type.Host);

        for (String imprt : this.imports) {
            sources.parse(imprt);
        }

        if (level == Level.Head) {
            for (Field f : fields) {
                Usable sc = sources.usable(f.usable);
                compiler.data(f.name, sc.size(sources));
            }

            for (Method m : methods) {
                Compiler.MethodCompiler mb = compiler.method();
                mb.name(m.name);
                for (Parameter p : m.params) {
                    Usable usable = sources.usable(p.usable);
                    mb.parameter(usable.name());
                }
            }
            for (String dependency : dependencies) {
                compiler.dependency(dependency);
            }

            return;
        }

        String administrator = null;
        for (String dependency : dependencies) {
            sources.parse(dependency);
            Document document = sources.document(dependency, Level.Head);

            compiler.dependency(dependency);
            if (administrator == null) {
                for (String documentImplementing : document.implementing) {
                   if (documentImplementing.equals("core.Administrator")) {
                       administrator = document.name;
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
            Usable sc = sources.usable(f.usable);
            compiler.data(f.name, sc.size(sources));
        }

        for (Method m : methods) {
            Compiler.MethodCompiler mb = compiler.method();
            Context context = new Context();
            mb.name(m.name);
            HashMap<String, Variable> variables = new HashMap<>();

            // declare the parameters
            for (Parameter p : m.params) {
                Usable usable = sources.usable(p.usable);
                mb.parameter(usable.name());
                usable.declare(mb, sources, variables, p.name, p.generics);
            }

            // handle each statement in the body
            for (Statement stmt : m.statements) {
                stmt.handle(mb, sources, variables, context);
            }
        }
    }

}
