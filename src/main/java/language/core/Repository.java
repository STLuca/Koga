package language.core;

import java.util.Optional;

public interface Repository {

    Optional<Structure> structure(String name);
    Optional<Compilable> compilable(String name);
    Optional<Document> document(String name);

}
