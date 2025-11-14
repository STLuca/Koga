package language.core;

import core.Types;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public interface Document {

    class Method {
        public List<String> parameters;
    }

    Types.Document type();
    String name();
    default Optional<Method> method(Scope scope, String name) {
        return Optional.empty();
    };
    default List<String> implementing() {
        return new ArrayList<>();
    };
    default Document withParameters(List<Scope.Generic> generics) {
        return null;
    };

}
