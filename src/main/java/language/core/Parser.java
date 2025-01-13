package language.core;

public interface Parser {

    String name();
    void parse(Classes classes, String input);

}
