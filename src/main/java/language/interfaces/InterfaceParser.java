package language.interfaces;

import language.core.Sources;
import language.core.Parser;
import language.scanning.Scanner;
import language.scanning.Token;
import language.scanning.Tokens;

public class InterfaceParser implements Parser {

    Token DEPENDENCIES, IMPORTS, CONSTRUCTOR, NAME, OP_BRACE, CL_BRACE, OP_PAREN, CL_PAREN,
            SEMI_COLON, NUMBER, TYPE, PARAM_TYPE, COMMA, LITERAL_ARG, OP_SQ_BRACKET,
            CL_SQ_BRACKET, EQUALS, DOT, OP_PT_BRACE, CL_PT_BRACE;
    Tokens tokens = new Tokens();

    {
        DEPENDENCIES  = tokens.add("'dependencies'");
        IMPORTS       = tokens.add("'imports'");
        CONSTRUCTOR   = tokens.add("'constructor'");
        TYPE          = tokens.add("'word'"); // what is this used for?
        PARAM_TYPE    = tokens.add("b[1-9][0-9]*");
        NAME          = tokens.add("[a-zA-Z]+");
        LITERAL_ARG   = tokens.add("0b[0-1]+|0x[0-9a-f]+|[0-9]+");
        NUMBER        = tokens.add("[1-9]+");
        EQUALS        = tokens.add("'='");
        OP_BRACE      = tokens.add("'{'");
        CL_BRACE      = tokens.add("'}'");
        OP_PAREN      = tokens.add("'('");
        CL_PAREN      = tokens.add("')'");
        OP_SQ_BRACKET = tokens.add("'['");
        CL_SQ_BRACKET = tokens.add("']'");
        OP_PT_BRACE   = tokens.add("'<'");
        CL_PT_BRACE   = tokens.add("'>'");
        SEMI_COLON    = tokens.add("';'");
        COMMA         = tokens.add("','");
        DOT           = tokens.add("'.'");
    }
    
    public String name() {
        return "interface";
    }
    
    public void parse(Sources sources, String input) {
        Scanner scanner = new Scanner(input);
        InterfaceCompilable ic = new InterfaceCompilable();

        Token curr = scanner.next(tokens);
        if (curr == IMPORTS) {
            scanner.expect(tokens, OP_BRACE);
            curr = scanner.next(tokens);
            while (curr != CL_BRACE) {
                if (curr != NAME) throw new RuntimeException("name");
                ic.imports.add(curr.matched());
                scanner.expect(tokens, SEMI_COLON);
                curr = scanner.next(tokens);
            }
            curr = scanner.next(tokens);
        }
        if (curr == DEPENDENCIES) {
            scanner.expect(tokens, OP_BRACE);
            curr = scanner.next(tokens);
            while (curr != CL_BRACE) {
                if (curr != NAME) throw new RuntimeException("name");
                ic.dependencies.add(curr.matched());
                scanner.expect(tokens, SEMI_COLON);
                curr = scanner.next(tokens);
            }
            curr = scanner.next(tokens);
        }
        if (curr != NAME) {
            scanner.fail("name");
        }
        ic.name = curr.matched();
        scanner.expect(tokens, OP_BRACE);
        curr = scanner.next(tokens);
        while (curr != CL_BRACE) {
            Token peek = scanner.peek(tokens).orElseThrow();
            parseMethod(scanner, ic);
            curr = scanner.next(tokens);
        }
        sources.add(ic);
    }

    private void parseMethod(Scanner scanner, InterfaceCompilable ic) {
        Token curr = scanner.current();
        Method m = new Method();
        m.name = curr.matched();
        scanner.expect(tokens, OP_PAREN);
        curr = scanner.next(tokens);
        while (curr != CL_PAREN) {
            if (curr != NAME) scanner.fail("name");
            Parameter p = new Parameter();
            p.Usable = curr.matched();
            scanner.expect(tokens, NAME);
            p.name = curr.matched();
            m.params.add(p);
            curr = scanner.next(tokens);
            if (curr == COMMA) curr = scanner.next(tokens);
        }
        scanner.expect(tokens, SEMI_COLON);
        ic.methods.add(m);
    }
}
