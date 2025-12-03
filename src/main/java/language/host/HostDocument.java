package language.host;

import core.Types;
import language.core.Document;
import language.core.Repository;
import language.core.Scope;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class HostDocument implements Document {

    HostCompilable host;

    public Types.Document type() {
        return Types.Document.Host;
    }

    public String name() {
        return host.name;
    }

    public Optional<Method> method(Scope.Description description, String name, Repository repository) {
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
