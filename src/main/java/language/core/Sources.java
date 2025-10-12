package language.core;

import core.Document;

public interface Sources {

    Sources root();
    boolean parse(String name);
    Usable usable(String name);
    Document document(String name, Compilable.Level level);
    void add(Usable c);
    void add(Compilable c);

}
