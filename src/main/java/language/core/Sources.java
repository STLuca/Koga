package language.core;

import core.Document;

public interface Sources {

    Sources root();
    boolean parse(String name);
    Usable usable(String name);
    Compilable compilable(String name);
    Document document(String name);
    void add(Usable c);
    void add(Document d);
    void add(Compilable c);

}
