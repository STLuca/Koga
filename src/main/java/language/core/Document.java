package language.core;

import core.Types;

import java.util.List;
import java.util.Optional;

public interface Document {

    class Method {
        public List<Scope.Description> parameters;
    }

    Types.Document type();
    String name();
    Optional<Method> method(Scope.Description description, String name, Repository repository);
    List<String> implementing();

}
