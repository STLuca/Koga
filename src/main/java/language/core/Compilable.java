package language.core;

import java.util.List;

public interface Compilable {

    String name();
    List<String> dependencies();
    Document document();
    void compile(Repository repository, Compiler compiler);

}
