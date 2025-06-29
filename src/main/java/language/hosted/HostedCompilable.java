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

    public void compile(Sources sources, Compiler compiler) {
        compiler.name(name);
        compiler.type(Document.Type.Hosted);

        RenamedSources myClasses = new RenamedSources(sources);

        for (Name imprt : this.imports) {
            sources.parse(imprt.globalName);
            Usable usable = sources.usable(imprt.globalName);
            myClasses.usables.put(imprt.localName, usable);
        }

        Document tempThisDoc = new Document();
        tempThisDoc.type = Document.Type.Host;
        tempThisDoc.name = name;
        tempThisDoc.methods = new Document.Method[methods.size()];
        for (int i = 0; i < methods.size(); i++) {
            Method m = methods.get(i);
            Document.Method dm = new Document.Method();
            dm.name = m.name;
            dm.parameters = new String[m.params.size()];
            for (int ii = 0; ii < m.params.size(); ii++) {
                dm.parameters[ii] = myClasses.usable(m.params.get(ii).usable).name();
            }
            tempThisDoc.methods[i] = dm;
        }
        String[] split = name.split("\\.");
        myClasses.documents.put(split[split.length - 1], tempThisDoc);
        myClasses.compilables.put(split[split.length - 1], this);

        for (Name dependency : dependencies) {
            sources.parse(dependency.globalName);
            Document document = sources.document(dependency.globalName);
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
