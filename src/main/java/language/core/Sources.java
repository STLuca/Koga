package language.core;

import core.Document;

public interface Sources {

    boolean parse(String name);
    Structure structure(String name);
    Document document(String name, Compilable.Level level);
    void add(Structure c);
    void add(Compilable c);

}
