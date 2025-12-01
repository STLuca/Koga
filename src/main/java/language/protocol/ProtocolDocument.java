package language.protocol;

import core.Types;
import language.core.Document;
import language.core.Repository;
import language.core.Scope;
import language.interfaces.Parameter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProtocolDocument implements Document {

    Protocol protocol;

    public Types.Document type() {
        return Types.Document.Protocol;
    }

    public String name() {
        return protocol.name;
    }

    public Optional<Method> method(Scope.Generic description, String name, Repository repository) {
        return Optional.empty();
    }

    public List<String> implementing() {
        return List.of();
    }

}
