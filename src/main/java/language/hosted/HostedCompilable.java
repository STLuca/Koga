package language.hosted;

import core.Document;
import language.core.*;
import language.core.Compiler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class HostedCompilable implements Compilable {

    String name;
    ArrayList<Name> imports = new ArrayList<>();
    ArrayList<Name> dependencies = new ArrayList<>();
    ArrayList<String> interfaces = new ArrayList<>();
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
        compiler.type(Document.Type.Hosted);

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
            HashMap<String, String> names = new HashMap<>();
            for (Name dependency : dependencies) {
                compiler.dependency(dependency.globalName);
                names.put(dependency.localName, dependency.globalName);
            }

            for (String implementing : interfaces) {
                compiler.implementing(names.get(implementing));
            }
            return;
        }

        for (Name dependency : dependencies) {
            sources.parse(dependency.globalName);
            Document document = sources.document(dependency.globalName, Level.Head);
            myClasses.documents.put(dependency.localName, document);
            compiler.dependency(dependency.globalName);
        }
        for (String intrface : interfaces) {
            Document implementing = myClasses.documents.get(intrface);
            compiler.implementing(implementing.name);
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

            // empty construct the parameters
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
