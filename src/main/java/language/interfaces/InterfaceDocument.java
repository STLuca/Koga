package language.interfaces;

import core.Types;
import language.core.Document;
import language.core.Scope;

import java.util.ArrayList;
import java.util.Optional;

public class InterfaceDocument implements Document {

    InterfaceCompilable host;

    public Types.Document type() {
        return Types.Document.Host;
    }

    public String name() {
        return host.name;
    }

    public Optional<Method> method(Scope scope, String name) {
        for (language.interfaces.Method m : host.methods) {
            if (m.name.equals(name)) {
                Method hostMethod = new Method();
                hostMethod.parameters = new ArrayList<>();
                for (Parameter p : m.params) {
                    hostMethod.parameters.add(p.descriptor.name);
                }
                return Optional.of(hostMethod);
            }
        }
        return Optional.empty();
    }

}
