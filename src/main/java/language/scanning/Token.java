package language.scanning;

public interface Token {

    boolean match(String input, int offset, boolean isTmp);
    String label();
    String matched();

}
