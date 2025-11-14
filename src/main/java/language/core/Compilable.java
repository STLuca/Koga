package language.core;

import java.util.List;

public interface Compilable {

    enum Level {
        Head,
        Full
    }

    String name();
    List<String> dependencies();
    default Document document() {
        throw new UnsupportedOperationException();
    };
    void compile(Repository repository, Compiler compiler, Level level);

}
