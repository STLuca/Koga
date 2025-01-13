package language.scanning;

import java.util.List;
import java.util.Optional;

// List<String> lines or String input?
// List maybe helps with debugging and fail message but is more complicated?
public class Scanner {

    List<String> lines;
    int inputLineNumber = 1;
    int inputPosition = 0;
    Token currentToken = null;

    public Scanner(String input) {
        this.lines = input.lines().toList();
    }

    public boolean isEnd() {
        return inputLineNumber >= lines.size() - 1 &&
                inputPosition == lines.get(inputLineNumber).length();
    }

    public Token current() {
        return currentToken;
    }

    public Optional<Token> peek(Tokens tokens) {
        if (isEnd()) {
            return Optional.empty();
        }

        int peekInputPosition = inputPosition;
        int peekInputLineNumber = inputLineNumber;
        String peekInputLine = lines.get(peekInputLineNumber);
        while (peekInputLine.length() <= peekInputPosition || peekInputLine.charAt(peekInputPosition) == ' ') {
            if (peekInputLine.length() <= peekInputPosition) {
                peekInputLineNumber++;
                peekInputLine = lines.get(peekInputLineNumber);
                peekInputPosition = 0;
            } else {
                peekInputPosition++;
            }
        }
        return tokens.find(peekInputLine, peekInputPosition, true);
    }

    public Token next(Tokens tokens) {
        String currentLine = lines.get(inputLineNumber);
        while (currentLine.length() <= inputPosition || currentLine.charAt(inputPosition) == ' ') {
            if (currentLine.length() <= inputPosition) {
                inputLineNumber++;
                currentLine = lines.get(inputLineNumber);
                inputPosition = 0;
            } else {
                inputPosition++;
            }
        }

        Token next = tokens.find(currentLine, inputPosition, false).orElse(null);
        if (next == null) fail("No token found");
        currentToken = next;
        inputPosition += currentToken.matched().length();
        return next;
    }

    public Token expect(Tokens tokens, Token expected) {
        Token next = next(tokens);
        if (next != expected) fail("Expecting " + expected.label() + " but found " + next.label());
        return next;
    }

    public void fail(String reason) {
        StringBuilder b = new StringBuilder();
        b.append("Failed to parse: ")
                .append(reason)
                .append("\n");
        if (inputLineNumber > 0) {
            b.append(lines.get(inputLineNumber - 2));
            b.append("\n");
        }
        b.append(lines.get(inputLineNumber));
        b.append("\n");
        for (int i = 0; i < inputPosition; i++) {
            b.append(" ");
        }
        b.append("^\n");
        if (inputLineNumber < lines.size() - 1){
            b.append(lines.get(inputLineNumber + 1));
        }
        throw new RuntimeException(b.toString());
    }

    public void takeUntilNewLine() {
        inputLineNumber++;
        inputPosition = 0;
    }
}
