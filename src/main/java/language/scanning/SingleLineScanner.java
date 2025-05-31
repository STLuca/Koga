package language.scanning;

import java.util.List;

public class SingleLineScanner {

    int inputPosition;
    String input;
    int[] positionByLine;
    Token currentToken = null;

    public SingleLineScanner(String input) {
        this.input = input;

        // calculate positionByLine
        List<String> lines = input.lines().toList();
        positionByLine = new int[lines.size()];
        int currPosition = 0;
        int lineIndex = 0;
        for (String line : lines) {
            positionByLine[lineIndex] = currPosition;
            lineIndex++;
            currPosition += line.length() + 1; // + 1 for newline
        }
        // skip parser line
        inputPosition = positionByLine[1];
    }

    public Token current() {
        return currentToken;
    }

    public Token peek(Tokens tokens) {
        int peekInputPosition = inputPosition;
        char c = input.charAt(peekInputPosition);
        while (c == ' ' || c == '\n') {
            peekInputPosition++;
            c = input.charAt(peekInputPosition);
        }
        return tokens.find(input, peekInputPosition, true).orElse(null);
    }

    public Token next(Tokens tokens) {
        char c = input.charAt(inputPosition);
        while (c == ' ' || c == '\n') {
            inputPosition++;
            c = input.charAt(inputPosition);
        }

        Token next = tokens.find(input, inputPosition, false).orElse(null);
        if (next == null) fail("No token found");
        currentToken = next;
        inputPosition += currentToken.matched().length();
        return next;
    }

    public Token expect(Token token) {
        char c = input.charAt(inputPosition);
        while (c == ' ' || c == '\n') {
            inputPosition++;
            c = input.charAt(inputPosition);
        }

        if (token.match(input, inputPosition, false)) {
            currentToken = token;
            inputPosition += currentToken.matched().length();
            return token;
        }
        fail("No token found");
        return null;
    }

    public Token expect(Tokens tokens, Token expected) {
        Token next = next(tokens);
        if (next != expected) fail("Expecting " + expected.label() + " but found " + next.label());
        return next;
    }

    public void fail(String reason) {
        StringBuilder b = new StringBuilder();
        int startOfLine = inputPosition - 1;
        while (input.charAt(startOfLine) != '\n') {
            startOfLine--;
        }
        int endOfLine = inputPosition;
        while (input.charAt(endOfLine) != '\n') {
            endOfLine++;
        }
        String currentLine = input.substring(startOfLine, endOfLine);
        b.append("Failed to parse: ")
                .append(reason)
                .append("\n")
                .append(currentLine);
        throw new RuntimeException(b.toString());
    }

    public void takeUntilNewLine() {
        while (input.charAt(inputPosition) != '\n') {
            inputPosition++;
        }
    }
}
