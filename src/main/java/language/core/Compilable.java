package language.core;

import java.util.List;

public interface Compilable {

    enum Level {
        Head,
        Full
    }

    String name();
    List<String> dependencies();
    void compile(Sources sources, Compiler compiler, Level level);

}
