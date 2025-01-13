package language.machine;

import language.core.Classes;
import language.core.Parser;
import language.scanning.Scanner;
import language.scanning.Token;
import language.scanning.Tokens;

import java.util.ArrayList;
import java.util.List;

public class MachineReferenceParser implements Parser {

    Token CONSTRUCTOR, NAME, OP_BRACE, CL_BRACE, OP_PAREN, CL_PAREN,
            SEMI_COLON, NUMBER, TYPE, ADDR, PARAM_TYPE, COMMA, LITERAL_ARG, OP_SQ_BRACKET,
            CL_SQ_BRACKET, DOT, OP_PT_BRACE, CL_PT_BRACE, TILDA;
    Tokens tokens = new Tokens();

    {
        CONSTRUCTOR   = tokens.add("'constructor'");
        TYPE          = tokens.add("'byte'");
        ADDR          = tokens.add("'Addr'");
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
        TILDA         = tokens.add("'~'");
    }

    
    public String name() {
        return "machineReference";
    }

    
    public void parse(Classes classes, String input) {
        Scanner scanner = new Scanner(tokens, input);
        MachineReferenceUsable mc = new MachineReferenceUsable();
        Token curr = scanner.current();
        if (curr != NAME) scanner.fail("name");
        mc.name = curr.matched();
        curr = scanner.next(tokens);
        if (curr == OP_PT_BRACE) {
            do {
                curr = scanner.next(tokens);
                if (curr != NAME) scanner.fail("name");
                mc.referencedClass = curr.matched();
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

    private void parseAddress(Scanner scanner, MachineReferenceUsable mc) {
        Token curr = scanner.expect(tokens, NAME);
        mc.addresses.add(curr.matched());
        scanner.expect(tokens, SEMI_COLON);
    }

    private void parseVariable(Scanner scanner, MachineReferenceUsable mc) {
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

    private void parseConstructor(Scanner scanner, MachineReferenceUsable mc) {
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
            String paramType = curr.matched();
            curr = scanner.expect(tokens, NAME);
            String paramName = curr.matched();
            if (binaryType) {
                mcc.addParam(Integer.parseInt(paramType.substring(1)), paramName, false);
            } else {
                mcc.addParam(paramType, paramName, false);
            }
            if (scanner.peek(tokens).orElse(null) == COMMA) scanner.next(tokens);
            curr = scanner.next(tokens);
        }
        scanner.expect(tokens, OP_BRACE);
        curr = scanner.next(tokens);
        parseBody(scanner, mc, mcc.body);
        mc.constructors.add(mcc);
    }

    private void parseMethod(Scanner scanner, MachineReferenceUsable mc) {
        Method mcm = new Method();
        Token curr = scanner.current();
        if (curr != NAME) scanner.fail("name");
        mcm.name = curr.matched();
        scanner.expect(tokens, OP_PAREN);
        curr = scanner.next(tokens);
        while (curr != CL_PAREN) {
            if (curr != PARAM_TYPE && curr != NAME) throw new RuntimeException("expecting parameter type");
            boolean binaryType = curr == PARAM_TYPE;
            String paramType = curr.matched();
            curr = scanner.expect(tokens, NAME);
            String paramName = curr.matched();
            if (binaryType) {
                mcm.addParam(Integer.parseInt(paramType.substring(1)), paramName, false);
            } else {
                mcm.addParam(paramType, paramName, false);
            }
            if (scanner.peek(tokens).orElse(null) == COMMA) scanner.next(tokens);
            curr = scanner.next(tokens);
        }
        scanner.expect(tokens, OP_BRACE);
        curr = scanner.next(tokens);
        parseBody(scanner, mc, mcm.body);
        if (mcm.name.equals("invoke")) {
            mc.invokeMethod = mcm;
        } else if (mcm.name.equals("arg")) {
            mc.argMethod = mcm;
        }
    }

    private void parseBody(Scanner scanner, MachineReferenceUsable mc, List<Statement> body) {
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
                body.add(as);
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
                        parseBody(scanner, mc, bs.elseBlock);
                    } else {
                        bs.elseBlock = null;
                    }
                    body.add(bs);
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
                        parseBody(scanner, mc, mi.block);
                    }
                    body.add(mi);
                    if (name.equals("args")) {
                        mc.argStatement = mi;
                    }
                }
            } else if (curr == TYPE) {
                DataStatement ds = new DataStatement();
                scanner.expect(tokens, OP_SQ_BRACKET);
                curr = scanner.expect(tokens, NUMBER);
                ds.size = Integer.parseInt(curr.matched());
                scanner.expect(tokens, CL_SQ_BRACKET);
                curr = scanner.expect(tokens, NAME);
                ds.name = curr.matched();
                body.add(ds);
            } else {
                scanner.fail("Bad statement");
            }
            scanner.expect(tokens, SEMI_COLON);
            curr = scanner.next(tokens);
        }
    }
}
