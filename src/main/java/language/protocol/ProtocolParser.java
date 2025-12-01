package language.protocol;

import language.core.Compilable;
import language.core.Parser;
import language.scanning.Scanner;
import language.scanning.Token;
import language.scanning.Tokens;

public class ProtocolParser implements Parser {

    Token BYTE, PERMISSION, NAME, GLOBAL_NAME, NUMBER,
          OP_BRACE, CL_BRACE, OP_PAREN, CL_PAREN, OP_SQ_BRACKET, CL_SQ_BRACKET,
          SEMI_COLON;
    Tokens tokens = new Tokens();

    {
        BYTE            = tokens.add("'Byte'");
        PERMISSION      = tokens.add("read|write|all");
        GLOBAL_NAME     = tokens.add("[a-zA-Z]+\\.[a-zA-Z]+(\\.[a-zA-Z]+)*");
        NAME            = tokens.add("[a-zA-Z]+");
        NUMBER          = tokens.add("4096");
        OP_BRACE        = tokens.add("'{'");
        CL_BRACE        = tokens.add("'}'");
        OP_PAREN        = tokens.add("'('");
        CL_PAREN        = tokens.add("')'");
        OP_SQ_BRACKET   = tokens.add("'['");
        CL_SQ_BRACKET   = tokens.add("']'");
        SEMI_COLON      = tokens.add("';'");
    }

    public String name() {
        return "protocol";
    }

    public Output parse(String input) {
        Scanner scanner = new Scanner(input);
        Protocol pc = new Protocol();

        Token curr = scanner.next(tokens);
        if (curr != GLOBAL_NAME) scanner.fail("name");
        pc.name = curr.matched();

        scanner.expect(tokens, OP_BRACE);
        curr = scanner.next(tokens);
        while (curr != CL_BRACE) {
            if (curr == NAME) {
                parseMethod(scanner, pc);
            } else {
                scanner.fail("Excepting name");
            }
            curr = scanner.next(tokens);
        }

        Output out = new Output();
        out.names = new String[] { pc.name };
        out.compilables = new Compilable[] { pc };
        return out;
    }

    void parseMethod(Scanner scanner, Protocol pc) {
        Method m = new Method();
        Token curr = scanner.current();
        if (curr != NAME) scanner.fail("method name");
        m.name = curr.matched();
        scanner.expect(tokens, OP_BRACE);
        curr = scanner.next(tokens);
        while (curr != CL_BRACE) {
            Method.Parameter parameter = new Method.Parameter();
            if (curr != BYTE) scanner.fail("byte");
            scanner.expect(tokens, OP_SQ_BRACKET);
            curr = scanner.expect(tokens, NUMBER);
            parameter.size = curr.matched();
            scanner.expect(tokens, CL_SQ_BRACKET);
            curr = scanner.expect(tokens, PERMISSION);
            parameter.permission = Method.Permission.valueOf(curr.matched().toUpperCase());
            scanner.expect(tokens, SEMI_COLON);
            curr = scanner.next(tokens);
            m.parameters.add(parameter);
        }
        pc.methods.add(m);
    }
}
