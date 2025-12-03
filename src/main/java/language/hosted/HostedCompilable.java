package language.hosted;

import core.Types;
import language.core.*;
import language.core.Compiler;
import language.scopes.ObjScope;

import java.util.ArrayDeque;
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

    public void compile(Repository repository, Compiler compiler) {
        compiler.name(name);
        compiler.type(Types.Document.Hosted);

        for (String dependency : dependencies) {
            compiler.dependency(dependency);
        }

        for (String intrface : interfaces) {
            compiler.implementing(intrface);
        }

        for (Field f : fields) {
            Structure sc = repository.structure(f.descriptor.name).orElseThrow();
            compiler.data(f.name, sc.size(repository));
        }

        Scope hostedScope = ObjScope.rootState();
        for (Generic g : generics) {
            Scope.Description generic = new Scope.Description();
            switch (g.type) {
                case Structure -> {
                    generic.type = Scope.Description.Type.Structure;
                }
                case null, default -> throw new RuntimeException();
            }
            GenericStructure genericStructure = new GenericStructure();
            genericStructure.name = g.name;
            generic.structure = genericStructure;
            hostedScope.put(g.name, generic);
        }

        for (Method m : methods) {
            Compiler.MethodCompiler mb = compiler.method();
            Scope scope = ObjScope.rootOperation(hostedScope);

            mb.name(m.name);

            for (Parameter p : m.params) {
                Structure structure = repository.structure(p.descriptor.name).orElseThrow();
                mb.parameter(p.descriptor.name);

                Scope.Description rootGeneric = new Scope.Description();
                ArrayDeque<Scope.Description> generics = new ArrayDeque<>();
                ArrayDeque<Descriptor> descriptors = new ArrayDeque<>();
                generics.push(rootGeneric);
                descriptors.push(p.descriptor);
                while (!generics.isEmpty()) {
                    Scope.Description g = generics.pop();
                    Descriptor d = descriptors.pop();
                    switch (d.type) {
                        case Structure -> {
                            g.type = Scope.Description.Type.Structure;
                            g.structure = repository.structure(d.name).orElseThrow();
                        }
                        case Document -> {
                            g.type = Scope.Description.Type.Document;
                            g.document = repository.document(d.name).orElseThrow();
                        }
                        case Generic -> {
                            g.type = Scope.Description.Type.Structure;
                            g.structure = hostedScope.findGeneric(d.name).orElseThrow().structure;
                        }
                    }
                    for (Descriptor subDescriptor : d.subDescriptors) {
                        descriptors.push(subDescriptor);
                        Scope.Description subGeneric = new Scope.Description();
                        g.generics.add(subGeneric);
                        generics.push(subGeneric);
                    }
                }
                
                structure.declare(mb, repository, scope, rootGeneric, p.name);
            }

            for (Statement stmt : m.statements) {
                stmt.handle(mb, repository, scope);
            }

            scope.debugData(mb);
        }
    }

}
