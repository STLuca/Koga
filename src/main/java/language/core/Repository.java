package language.core;

public interface Repository {

    Structure structure(String name);
    Compilable compilable(String name);
    Document document(String name);

}
