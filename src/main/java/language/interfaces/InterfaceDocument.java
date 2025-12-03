package language.interfaces;

import core.Types;
import language.core.Document;
import language.core.Repository;
import language.core.Scope;

import java.util.*;

public class InterfaceDocument implements Document {

    InterfaceCompilable host;

    public Types.Document type() {
        return Types.Document.Host;
    }

    public String name() {
        return host.name;
    }

    public Optional<Method> method(Scope.Description description, String name, Repository repository) {
        language.interfaces.Method method = null;
        for (language.interfaces.Method m : host.methods) {
            if (m.name.equals(name)) {
                method = m;
                break;
            }
        }
        if (method == null) {
            return Optional.empty();
        }
        HashMap<String, Scope.Description> genericsByName = new HashMap<>();
        for (int i = 0; i < host.generics.size(); i++) {
            genericsByName.put(host.generics.get(i).name, description.generics.get(i));
        }

        Method hostMethod = new Method();
        hostMethod.parameters = new ArrayList<>();
        for (Parameter p : method.params) {
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
                        Scope.Description generic = genericsByName.get(d.name);
                        if (generic == null) {
                            throw new RuntimeException();
                        }
                        g.type = generic.type;
                        g.document = generic.document;
                        g.structure = generic.structure;
                        g.generics = generic.generics;
                    }
                }
                for (Descriptor subDescriptor : d.subDescriptors) {
                    descriptors.push(subDescriptor);
                    Scope.Description subGeneric = new Scope.Description();
                    g.generics.add(subGeneric);
                    generics.push(subGeneric);
                }
            }
            hostMethod.parameters.add(rootGeneric);
        }
        return Optional.of(hostMethod);
    }

    public List<String> implementing() {
        return List.of();
    }
}
