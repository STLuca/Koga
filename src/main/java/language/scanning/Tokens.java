package language.scanning;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Tokens {

    ArrayList<Token> tokens = new ArrayList<>();

    public void add(Token token) {
        tokens.add(token);
    }

    public void add(Token... tokens) {
        this.tokens.addAll(Arrays.asList(tokens));
    }

    public Token add(String spec) {
        return add(spec, "unknown");
    }

    public Token add(String spec, String label) {
        Token token = create(spec, label);
        tokens.add(token);
        return token;
    }

    public Token create(String spec, String label) {
        Token token;
        if (spec.startsWith("'") && spec.endsWith("'")) {
            token = new LiteralToken(spec.substring(1, spec.length() - 1));
        } else {
            token = new RegexToken(spec, label);
        }
        return token;
    }

    public Optional<Token> find(String input, int offset, boolean isTmp) {
        for (Token token : tokens) {
            if (token.match(input, offset, isTmp)) {
                return Optional.of(token);
            }
        }
        return Optional.empty();
    }

    private static class LiteralToken implements Token {

        String literal;

        public LiteralToken(String literal) {
            this.literal = literal;
        }
        
        public boolean match(String input, int offset, boolean isTmp) {
            return input.startsWith(literal, offset);
        }
        
        public String label() {
            return literal;
        }
        
        public String matched() {
            return literal;
        }

    }

    private static class RegexToken implements Token {

        Pattern regex;
        Matcher matcher;
        String label;

        public RegexToken(String regex, String label) {
            this.regex = Pattern.compile(regex);
            this.label = label;
        }
        
        public boolean match(String input, int offset, boolean isTmp) {
            Matcher matcher = regex.matcher(input.substring(offset));
            if (!isTmp) {
                this.matcher = matcher;
            }
            return matcher.lookingAt();
        }

        public String label() {
            return label;
        }
        
        public String matched() {
            return matcher.group();
        }

    }

}
