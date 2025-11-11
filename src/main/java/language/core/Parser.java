package language.core;

public interface Parser {

    class Output {
        public Structure[] structures;
        public Compilable[] compilables;
    }

    String name();
    Output parse(String input);

}
