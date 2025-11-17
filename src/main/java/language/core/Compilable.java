package language.core;

import java.util.List;

public interface Compilable {

    String name();
    List<String> dependencies();
    default Document document() {
        throw new UnsupportedOperationException();
    };
    void compile(Repository repository, Compiler compiler);

}
