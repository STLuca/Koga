package language.interfaces;

import core.Types;
import language.core.Document;
import language.core.Repository;
import language.core.Scope;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;

public class InterfaceDocument implements Document {

    InterfaceCompilable host;

    public Types.Document type() {
        return Types.Document.Host;
    }

    public String name() {
        return host.name;
    }

    public Optional<Method> method(Scope.Generic description, String name, Repository repository) {
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
        HashMap<String, Scope.Generic> genericsByName = new HashMap<>();
        for (int i = 0; i < host.generics.size(); i++) {
            genericsByName.put(host.generics.get(i).name, description.generics.get(i));
        }

        Method hostMethod = new Method();
        hostMethod.parameters = new ArrayList<>();
        for (Parameter p : method.params) {
            Scope.Generic rootGeneric = new Scope.Generic();
            ArrayDeque<Scope.Generic> generics = new ArrayDeque<>();
            ArrayDeque<Descriptor> descriptors = new ArrayDeque<>();
            generics.push(rootGeneric);
            descriptors.push(p.descriptor);
            while (!generics.isEmpty()) {
                Scope.Generic g = generics.pop();
                Descriptor d = descriptors.pop();
                switch (d.type) {
                    case Structure -> {
                        g.type = Scope.Generic.Type.Structure;
                        g.structure = repository.structure(d.name).orElseThrow();
                    }
                    case Document -> {
                        g.type = Scope.Generic.Type.Document;
                        g.document = repository.document(d.name).orElseThrow();
                    }
                    case Generic -> {
                        Scope.Generic generic = genericsByName.get(d.name);
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
                    Scope.Generic subGeneric = new Scope.Generic();
                    g.generics.add(subGeneric);
                    generics.push(subGeneric);
                }
            }
            hostMethod.parameters.add(rootGeneric);
        }
        return Optional.of(hostMethod);
    }

}
