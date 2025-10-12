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
    ArrayList<Name> imports = new ArrayList<>();
    ArrayList<Name> dependencies = new ArrayList<>();
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
        for (Name dependency : this.dependencies) {
            dependencies.add(dependency.globalName);
        }
        return dependencies;
    }

    public void compile(Sources sources, Compiler compiler, Level level) {
        compiler.name(name);
        compiler.type(Document.Type.Host);

        RenamedSources myClasses = new RenamedSources(sources);

        for (Name imprt : this.imports) {
            sources.parse(imprt.globalName);
            Usable usable = sources.usable(imprt.globalName);
            myClasses.usables.put(imprt.localName, usable);
        }

        if (level == Level.Head) {
            for (Field f : fields) {
                Usable sc = myClasses.usable(f.usable);
                compiler.data(f.name, sc.size(sources));
            }

            for (Method m : methods) {
                Compiler.MethodCompiler mb = compiler.method();
                mb.name(m.name);
                for (Parameter p : m.params) {
                    Usable usable = myClasses.usable(p.usable);
                    mb.parameter(usable.name());
                }
            }
            for (Name dependency : dependencies) {
                compiler.dependency(dependency.globalName);
            }

            return;
        }

        String administrator = null;
        for (Name dependency : dependencies) {
            sources.parse(dependency.globalName);
            Document document = sources.document(dependency.globalName, Level.Head);
            myClasses.documents.put(dependency.localName, document);

            compiler.dependency(dependency.globalName);
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
            Usable sc = myClasses.usable(f.usable);
            compiler.data(f.name, sc.size(sources));
        }

        for (Method m : methods) {
            Compiler.MethodCompiler mb = compiler.method();
            Context context = new Context();
            mb.name(m.name);
            HashMap<String, Variable> variables = new HashMap<>();

            // declare the parameters
            for (Parameter p : m.params) {
                Usable usable = myClasses.usable(p.usable);
                mb.parameter(usable.name());
                usable.declare(mb, myClasses, variables, p.name, p.generics);
            }

            // handle each statement in the body
            for (Statement stmt : m.statements) {
                stmt.handle(mb, myClasses, variables, context);
            }
        }
    }

}
