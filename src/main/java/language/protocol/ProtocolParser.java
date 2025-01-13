package language.protocol;

import language.core.Classes;
import language.core.Parser;
import language.scanning.Scanner;
import language.scanning.Token;
import language.scanning.Tokens;

public class ProtocolParser implements Parser {

    Token DISTINGUISH, DEPENDENCIES, IMPORTS, NAME, OP_BRACE, CL_BRACE, OP_PAREN, CL_PAREN, SEMI_COLON;
    Tokens tokens = new Tokens();

    {
        DISTINGUISH   = tokens.add("'protocol class'");
        DEPENDENCIES  = tokens.add("'dependencies'");
        IMPORTS       = tokens.add("'imports'");
        NAME          = tokens.add("[a-zA-Z]+");
        OP_BRACE      = tokens.add("'{'");
        CL_BRACE      = tokens.add("'}'");
        OP_PAREN      = tokens.add("'('");
        CL_PAREN      = tokens.add("')'");
        SEMI_COLON    = tokens.add("';'");
    }

    
    public String name() {
        return "protocol";
    }

    
    public void parse(Classes classes, String input) {
        Scanner scanner = new Scanner(tokens, input);
        Protocol pc = new Protocol();

        Token curr = scanner.expect(tokens, NAME);
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

        classes.add(pc);
    }

    void parseMethod(Scanner scanner, Protocol pc) {
        Method m = new Method();
        Token curr = scanner.current();
        if (curr != NAME) scanner.fail("method name");
        m.name = curr.matched();
        scanner.expect(tokens, OP_PAREN);
        curr = scanner.next(tokens);
        while (curr != CL_PAREN) {
            curr = scanner.next(tokens);
        }
        scanner.expect(tokens, SEMI_COLON);
        pc.methods.add(m);
    }
}
