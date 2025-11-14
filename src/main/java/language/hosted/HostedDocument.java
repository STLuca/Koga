package language.hosted;

import core.Types;
import language.core.Document;
import language.core.Scope;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class HostedDocument implements Document {

    HostedCompilable host;

    public Types.Document type() {
        return Types.Document.Host;
    }

    public String name() {
        return host.name;
    }

    public Optional<Method> method(Scope scope, String name) {
        for (language.hosted.Method m : host.methods) {
            if (m.name.equals(name)) {
                Method hostMethod = new Method();
                hostMethod.parameters = new ArrayList<>();
                for (Parameter p : m.params) {
                    hostMethod.parameters.add(p.structure);
                }
                return Optional.of(hostMethod);
            }
        }
        return Optional.empty();
    }

    public List<String> implementing() {
        return host.interfaces;
    }
}
