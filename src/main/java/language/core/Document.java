package language.core;

import core.Types;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public interface Document {

    class Method {
        public List<Scope.Generic> parameters;
    }

    Types.Document type();
    String name();
    Optional<Method> method(Scope.Generic description, String name, Repository repository);
    List<String> implementing();

}
