package language.core;

public interface Parser {

    class Output {
        public String[] names;
        public Structure[] structures;
        public Compilable[] compilables;
    }

    String name();
    Output parse(String input);

}
