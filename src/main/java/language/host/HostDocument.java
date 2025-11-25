package language.host;

import core.Types;
import language.core.Document;
import language.core.Repository;
import language.core.Scope;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Optional;

public class HostDocument implements Document {

    HostCompilable host;

    public Types.Document type() {
        return Types.Document.Host;
    }

    public String name() {
        return host.name;
    }

    public Optional<Method> method(Scope.Generic description, String name, Repository repository) {
        language.host.Method method = null;
        for (language.host.Method m : host.methods) {
            if (m.name.equals(name)) {
                method = m;
                break;
            }
        }
        if (method == null) {
            return Optional.empty();
        }

        language.core.Document.Method hostMethod = new language.core.Document.Method();
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
                        g.structure = repository.structure(d.name);
                    }
                    case Document -> {
                        g.type = Scope.Generic.Type.Document;
                        g.document = repository.document(d.name);
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
