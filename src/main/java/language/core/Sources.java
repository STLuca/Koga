package language.core;

import core.Document;

public interface Sources {

    Structure structure(String name);
    Document document(String name, Compilable.Level level);

}
