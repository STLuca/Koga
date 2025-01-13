package language.core;

public interface Parser {

    String name();
    void parse(Sources sources, String input);

}
