package language.machine;

import language.core.Sources;
import language.core.Parser;
import language.scanning.Scanner;
import language.scanning.Token;
import language.scanning.Tokens;

public class MachineEnumParser implements Parser {

    Token LITERALS, NAME, OP_BRACE, CL_BRACE, OP_PAREN, CL_PAREN,
            SEMI_COLON, NUMBER, TYPE, ADDR, PARAM_TYPE, PARAM_ARRAY, COMMA, LITERAL_ARG, OP_SQ_BRACKET,
            CL_SQ_BRACKET, DOT, OP_PT_BRACE, CL_PT_BRACE;
    Tokens tokens = new Tokens();

     {
        LITERALS      = tokens.add("'literals'");
        TYPE          = tokens.add("'Byte'");
        PARAM_TYPE    = tokens.add("b[1-9][0-9]*");
        NAME          = tokens.add("[a-zA-Z_]+");
        LITERAL_ARG   = tokens.add("0b[0-1]+|0x[0-9a-f]+|0d[0-9]+|\\\"[a-zA-Z_]+\\\"");
        NUMBER        = tokens.add("[0-9]+");
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
        return "machineEnum";
    }

    
    public void parse(Sources sources, String input) {
        Scanner scanner = new Scanner(input);
        MachineEnumUsable mec = new MachineEnumUsable();
        Token curr = scanner.next(tokens);
        if (curr != LITERALS) scanner.fail("literals");
        curr = scanner.expect(tokens, OP_BRACE);
        curr = scanner.next(tokens);
        while (curr != CL_BRACE) {
            MachineEnumUsable.Literal l = new MachineEnumUsable.Literal();
            if (curr != NAME) scanner.fail("name");
            l.name = curr.matched();
            curr = scanner.expect(tokens, NUMBER);
            l.value = curr.matched();
            mec.literals.add(l);
            scanner.expect(tokens, SEMI_COLON);
            curr = scanner.next(tokens);
        }

        curr = scanner.expect(tokens, NAME);
        mec.name = curr.matched();

        scanner.expect(tokens, OP_BRACE);
        scanner.expect(tokens, TYPE);
        scanner.expect(tokens, OP_SQ_BRACKET);

        Data data = new Data();
        curr = scanner.expect(tokens, NUMBER);
        data.size = Integer.parseInt(curr.matched());
        scanner.expect(tokens, CL_SQ_BRACKET);
        curr = scanner.expect(tokens, NAME);
        data.name = curr.matched();
        mec.data = data;

        sources.add(mec);
    }
}
