package language.machine;

import language.core.Parser;
import language.core.Structure;
import language.scanning.Scanner;
import language.scanning.Token;
import language.scanning.Tokens;

public class MachineEnumParser implements Parser {

    Token LITERALS, NAME, GLOBAL_NAME, OP_BRACE, CL_BRACE, OP_PAREN, CL_PAREN,
            SEMI_COLON, NUMBER, TYPE, ADDR, PARAM_TYPE, PARAM_ARRAY, COMMA, LITERAL_ARG, OP_SQ_BRACKET,
            CL_SQ_BRACKET, DOT, OP_PT_BRACE, CL_PT_BRACE;
    Tokens tokens = new Tokens();

     {
        LITERALS      = tokens.add("'literals'");
        TYPE          = tokens.add("'Byte'");
        PARAM_TYPE    = tokens.add("b[1-9][0-9]*");
        GLOBAL_NAME   = tokens.add("[a-zA-Z]+\\.[a-zA-Z]+(\\.[a-zA-Z]+)*");
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

    
    public Output parse(String input) {
        Scanner scanner = new Scanner(input);
        MachineEnumStructure mec = new MachineEnumStructure();

        Token curr = scanner.expect(tokens, GLOBAL_NAME);
        mec.name = curr.matched();

        scanner.expect(tokens, OP_BRACE);
        curr = scanner.next(tokens);
        int currLiteral = 0;
        while (curr != CL_BRACE) {
            if (curr != NAME) scanner.fail("name");
            MachineEnumStructure.Literal l = new MachineEnumStructure.Literal();
            l.name = curr.matched();
            l.value = String.valueOf(currLiteral++);
            mec.literals.add(l);
            scanner.expect(tokens, OP_BRACE);
            scanner.expect(tokens, CL_BRACE);
            curr = scanner.next(tokens);
        }

        Data d = new Data();
        d.size = 1;
        d.name = "val";
        mec.data = d;


        Output out = new Output();
        out.structures = new Structure[] { mec };
        return out;
    }
}
