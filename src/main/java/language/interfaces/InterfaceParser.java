package language.interfaces;

import language.core.Compilable;
import language.core.Parser;
import language.scanning.Scanner;
import language.scanning.Token;
import language.scanning.Tokens;

import java.util.HashMap;

public class InterfaceParser implements Parser {

    static class Context {
        InterfaceCompilable ic;
        HashMap<String, String> structures = new HashMap<>();
    }

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
        IMPORTS       = metaTokens.add("'structures'");
        GLOBAL_NAME = metaTokens.add("[a-zA-Z]+\\.[a-zA-Z]+(\\.[a-zA-Z]+)*");
        metaTokens.add(NAME);
    }
    
    public String name() {
        return "interface";
    }
    
    public Output parse(String input) {
        Scanner scanner = new Scanner(input);
        InterfaceCompilable ic = new InterfaceCompilable();
        Context ctx = new Context();
        ctx.ic = ic;

        Token curr = scanner.next(metaTokens);
        if (curr == IMPORTS) {
            scanner.expect(tokens, OP_BRACE);
            curr = scanner.next(metaTokens);
            while (curr != CL_BRACE) {
                if (curr != GLOBAL_NAME) throw new RuntimeException("name");
                String globalName = curr.matched();
                String[] split = globalName.split("\\.");
                String localName = split[split.length - 1];
                curr = scanner.next(metaTokens);
                if (curr == NAME) {
                    localName = curr.matched();
                    curr = scanner.next(metaTokens);
                }
                if (curr != SEMI_COLON) { scanner.fail(";"); }
                ctx.structures.put(localName, globalName);
                ctx.ic.imports.add(globalName);
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
            parseMethod(scanner, ctx);
            curr = scanner.next(tokens);
        }

        Output out = new Output();
        out.compilables = new Compilable[] { ic };
        return out;
    }

    private void parseMethod(Scanner scanner, Context ctx) {
        Token curr = scanner.current();
        Method m = new Method();
        m.name = curr.matched();
        scanner.expect(tokens, OP_PAREN);
        curr = scanner.next(tokens);
        while (curr != CL_PAREN) {
            if (curr != NAME) scanner.fail("name");
            Parameter p = new Parameter();
            p.structure = ctx.structures.get(curr.matched());
            scanner.expect(tokens, NAME);
            p.name = curr.matched();
            m.params.add(p);
            curr = scanner.next(tokens);
            if (curr == COMMA) curr = scanner.next(tokens);
        }
        scanner.expect(tokens, SEMI_COLON);
        ctx.ic.methods.add(m);
    }
}
