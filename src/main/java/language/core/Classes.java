package language.core;

public interface Classes {

    Usable usable(String name);
    Compilable compilable(String name);
    void add(Usable c);
    void add(Compilable c);

}
