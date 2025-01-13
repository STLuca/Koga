package language.core;

import java.util.List;

public interface Compilable {

    String name();
    List<String> dependencies();
    void compile(Classes classes, Compiler compiler);

}
