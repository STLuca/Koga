package language.machine;

import language.core.Classes;
import language.core.Parser;
import language.scanning.Scanner;
import language.scanning.Token;
import language.scanning.Tokens;

import java.util.ArrayList;
import java.util.List;

/*

    machine class
    variables


    class name -> constructor = className()
    else -> method



 */
public class MachineUsableParser implements Parser {

    Token CONSTRUCTOR, NAME, OP_BRACE, CL_BRACE, OP_PAREN, CL_PAREN,
            SEMI_COLON, NUMBER, TYPE, ADDR, PARAM_TYPE, PARAM_ARRAY, COMMA, LITERAL_ARG, OP_SQ_BRACKET,
            CL_SQ_BRACKET, DOT, OP_PT_BRACE, CL_PT_BRACE, TILDA;
    Tokens tokens = new Tokens();

    {
        CONSTRUCTOR   = tokens.add("'constructor'");
        TYPE          = tokens.add("'byte'");
        ADDR          = tokens.add("'Addr'");
        PARAM_TYPE    = tokens.add("b[1-9][0-9]*");
        PARAM_ARRAY   = tokens.add("\\[\\]");
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
        TILDA         = tokens.add("'~'");
    }

    
    public String name() {
        return "machine";
    }

    
    public void parse(Classes classes, String input) {
        Scanner scanner = new Scanner(tokens, input);
        MachineUsable mc = new MachineUsable();
        Token curr = scanner.current();
        if (curr != NAME) scanner.fail("name");
        mc.name = curr.matched();
        curr = scanner.next(tokens);
        if (curr == OP_PT_BRACE) {
            do {
                curr = scanner.next(tokens);
                if (curr != NAME) scanner.fail("name");
                mc.generics.add(curr.matched());
                curr = scanner.next(tokens);
            } while (curr == COMMA);
            if (curr != CL_PT_BRACE) scanner.fail(">");
            curr = scanner.next(tokens);
        }
        if (curr != OP_BRACE) scanner.fail("{");
        curr = scanner.next(tokens);
        while (curr != CL_BRACE) {
            if (curr == TYPE) {
                parseVariable(scanner, mc);
            } else if (curr == ADDR) {
                parseAddress(scanner, mc);
            } else if (curr == CONSTRUCTOR) {
                parseConstructor(scanner, mc);
            } else if (curr == NAME) {
                parseMethod(scanner, mc);
            } else if (curr == TILDA) {
                scanner.takeUntilNewLine();
            }
            curr = scanner.next(tokens);
        }
        classes.add(mc);
    }

    private void parseAddress(Scanner scanner, MachineUsable mc) {
        Token curr = scanner.expect(tokens, NAME);
        mc.addresses.add(curr.matched());
        scanner.expect(tokens, SEMI_COLON);
    }

    private void parseVariable(Scanner scanner, MachineUsable mc) {
        Token curr = scanner.current();
        Data data = new Data();
        data.type = Data.Type.valueOf(curr.matched().toUpperCase());
        curr = scanner.next(tokens);
        if (curr == OP_SQ_BRACKET) {
            curr = scanner.expect(tokens, NUMBER);
            data.size = Integer.parseInt(curr.matched());
            scanner.expect(tokens, CL_SQ_BRACKET);
            curr = scanner.next(tokens);
        }
        if (curr != NAME) throw new RuntimeException("Expected name");
        data.name = curr.matched();
        scanner.expect(tokens, SEMI_COLON);
        mc.variables.add(data);
    }

    private void parseConstructor(Scanner scanner, MachineUsable mc) {
        Constructor mcc = new Constructor();
        Token curr = scanner.next(tokens);
        if (curr == NAME) {
            mcc.name = curr.matched();
            scanner.expect(tokens, OP_PAREN);
        } else if (curr == OP_PAREN) {
            mcc.name = "";
        } else {
            scanner.fail("(");
        }
        curr = scanner.next(tokens);
        while (curr != CL_PAREN) {
            if (curr != PARAM_TYPE && curr != NAME) throw new RuntimeException("expecting parameter type");
            boolean binaryType = curr == PARAM_TYPE;
            boolean isArray = false;
            String paramType = curr.matched();
            curr = scanner.next(tokens);
            if (curr == PARAM_ARRAY) {
                isArray = true;
                curr = scanner.next(tokens);
            }
            if (curr != NAME) scanner.fail("name");
            String paramName = curr.matched();
            if (binaryType) {
                mcc.addParam(Integer.parseInt(paramType.substring(1)), paramName, isArray);
            } else {
                mcc.addParam(paramType, paramName, isArray);
            }
            if (scanner.peek(tokens).orElse(null) == COMMA) scanner.next(tokens);
            curr = scanner.next(tokens);
        }
        scanner.expect(tokens, OP_BRACE);
        scanner.next(tokens);
        parseStatements(scanner, mcc.body);
        mc.constructors.add(mcc);
    }

    private void parseMethod(Scanner scanner, MachineUsable mc) {
        Method mcm = new Method();
        Token curr = scanner.current();
        if (curr != NAME) scanner.fail("name");
        mcm.name = curr.matched();
        scanner.expect(tokens, OP_PAREN);
        curr = scanner.next(tokens);
        while (curr != CL_PAREN) {
            if (curr != PARAM_TYPE && curr != NAME) throw new RuntimeException("expecting parameter type");
            boolean binaryType = curr == PARAM_TYPE;
            boolean isArray = false;
            String paramType = curr.matched();
            curr = scanner.next(tokens);
            if (curr == PARAM_ARRAY) {
                isArray = true;
                curr = scanner.next(tokens);
            }
            if (curr != NAME) scanner.fail("name");
            String paramName = curr.matched();
            if (binaryType) {
                mcm.addParam(Integer.parseInt(paramType.substring(1)), paramName, isArray);
            } else {
                mcm.addParam(paramType, paramName, isArray);
            }
            if (scanner.peek(tokens).orElse(null) == COMMA) scanner.next(tokens);
            curr = scanner.next(tokens);
        }
        scanner.expect(tokens, OP_BRACE);
        scanner.next(tokens);
        parseStatements(scanner, mcm.body);
        mc.methods.add(mcm);
    }

    void parseStatements(Scanner scanner, List<Statement> statements) {
        Token curr = scanner.current();
        while (curr != CL_BRACE) {
            if (curr == TILDA) {
                scanner.takeUntilNewLine();
                curr = scanner.next(tokens);
                continue;
            }

            if (curr == ADDR) {
                // Address statement
                AddressStatement as = new AddressStatement();
                curr = scanner.expect(tokens, NAME);
                as.name = curr.matched();
                statements.add(as);
            } else if (curr == NAME) {
                String name = curr.matched();
                Token peek = scanner.peek(tokens).orElse(null);
                if (peek == SEMI_COLON || peek == OP_BRACE) {
                    // Block statement
                    BlockStatement bs = new BlockStatement();
                    bs.name = name;
                    if (peek == OP_BRACE) {
                        scanner.next(tokens);
                        scanner.next(tokens);
                        parseStatements(scanner, bs.elseBlock);
                    } else {
                        bs.elseBlock = null;
                    }
                    statements.add(bs);
                } else if (peek == OP_PAREN) {
                    InstructionStatement mi = new InstructionStatement(name);
                    scanner.expect(tokens, OP_PAREN);
                    curr = scanner.next(tokens);
                    while (curr != CL_PAREN) {
                        if (curr == LITERAL_ARG) {
                            mi.arguments.add(curr.matched());
                        } else if (curr == NAME) {
                            String fullVal = "";
                            while (curr == NAME) {
                                fullVal += curr.matched();
                                if (scanner.peek(tokens).orElse(null) != DOT) break;
                                scanner.next(tokens);
                                curr = scanner.expect(tokens, NAME);
                                fullVal += ".";
                            }
                            mi.arguments.add(fullVal);
                        }
                        if (scanner.peek(tokens).orElse(null) == COMMA) scanner.next(tokens);
                        curr = scanner.next(tokens);
                    }
                    peek = scanner.peek(tokens).orElseThrow();
                    if (peek == OP_BRACE) {
                        scanner.next(tokens);
                        scanner.next(tokens);
                        mi.block = new ArrayList<>();
                        parseStatements(scanner, mi.block);
                    }
                    statements.add(mi);
                }
            } else if (curr == TYPE) {
                DataStatement ds = new DataStatement();
                scanner.expect(tokens, OP_SQ_BRACKET);
                curr = scanner.expect(tokens, NUMBER);
                ds.size = Integer.parseInt(curr.matched());
                scanner.expect(tokens, CL_SQ_BRACKET);
                curr = scanner.expect(tokens, NAME);
                ds.name = curr.matched();
                statements.add(ds);
            } else {
                scanner.fail("Bad statement");
            }
            scanner.expect(tokens, SEMI_COLON);
            curr = scanner.next(tokens);
        }
    }

}
