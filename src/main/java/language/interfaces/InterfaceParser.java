package language.interfaces;

import language.core.Sources;
import language.core.Parser;
import language.scanning.Scanner;
import language.scanning.Token;
import language.scanning.Tokens;

public class InterfaceParser implements Parser {

    Token   DEPENDENCIES, IMPORTS,
            NAME, GLOBAL_NAME,
            SEMI_COLON, COMMA, EQUALS, DOT,
            OP_BRACE, CL_BRACE, OP_PAREN, CL_PAREN, OP_SQ_BRACKET, CL_SQ_BRACKET, OP_PT_BRACE, CL_PT_BRACE;
    Tokens tokens = new Tokens();
    Tokens metaTokens = new Tokens();

    {
        NAME          = tokens.add("[a-zA-Z]+");
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

        metaTokens.add(OP_BRACE);
        metaTokens.add(CL_BRACE);
        metaTokens.add(SEMI_COLON);
        DEPENDENCIES  = metaTokens.add("'dependencies'");
        IMPORTS       = metaTokens.add("'usables'");
        GLOBAL_NAME = metaTokens.add("[a-zA-Z]+\\.[a-zA-Z]+(\\.[a-zA-Z]+)*");
        metaTokens.add(NAME);
    }
    
    public String name() {
        return "interface";
    }
    
    public void parse(Sources sources, String input) {
        Scanner scanner = new Scanner(input);
        InterfaceCompilable ic = new InterfaceCompilable();

        Token curr = scanner.next(metaTokens);
        if (curr == IMPORTS) {
            scanner.expect(tokens, OP_BRACE);
            curr = scanner.next(metaTokens);
            while (curr != CL_BRACE) {
                if (curr != GLOBAL_NAME) throw new RuntimeException("name");
                ic.imports.add(curr.matched());
                scanner.expect(tokens, SEMI_COLON);
                curr = scanner.next(metaTokens);
            }
            curr = scanner.next(metaTokens);
        }
        if (curr == DEPENDENCIES) {
            scanner.expect(tokens, OP_BRACE);
            curr = scanner.next(metaTokens);
            while (curr != CL_BRACE) {
                if (curr != NAME) throw new RuntimeException("name");
                ic.dependencies.add(curr.matched());
                scanner.expect(tokens, SEMI_COLON);
                curr = scanner.next(metaTokens);
            }
            curr = scanner.next(metaTokens);
        }
        if (curr != GLOBAL_NAME && curr != NAME) {
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
