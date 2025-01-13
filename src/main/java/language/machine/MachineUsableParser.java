package language.machine;

import language.core.Sources;
import language.core.Parser;
import language.scanning.SingleLineScanner;
import language.scanning.Token;
import language.scanning.Tokens;

import java.util.List;

public class MachineUsableParser implements Parser {

    Token CONSTRUCTOR, NAME, BYTE, ADDR, POSITION, BLOCK, ADMIN,
          NUMBER, LITERAL_ARG, PARAM_TYPE, PARAM_ARRAY,
          OP_BRACE, CL_BRACE, OP_PAREN, CL_PAREN, OP_SQ_BRACKET, CL_SQ_BRACKET, OP_PT_BRACE, CL_PT_BRACE,
          SEMI_COLON, COMMA, DOT, TILDA;
    Tokens tokens = new Tokens();

    {
        CONSTRUCTOR   = tokens.add("'constructor'");
        BYTE          = tokens.add("'byte'");
        ADDR          = tokens.add("'Addr'");
        POSITION      = tokens.add("'Position'");
        BLOCK         = tokens.add("'Blockk'");
        ADMIN         = tokens.add("'ADMIN'");
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
    
    public void parse(Sources sources, String input) {
        SingleLineScanner scanner = new SingleLineScanner(input);
        MachineUsable mc = new MachineUsable();
        Token curr = scanner.next(tokens);
        if (curr != NAME) scanner.fail("name");
        mc.name = curr.matched();
        curr = scanner.next(tokens);
        if (curr == OP_PT_BRACE) {
            do {
                curr = scanner.expect(tokens, NAME);
                String type = curr.matched();
                curr = scanner.expect(tokens, NAME);
                String name = curr.matched();
                Generic g = new Generic();
                g.type = Generic.Type.valueOf(type);
                g.name = name;
                mc.generics.add(g);
                curr = scanner.next(tokens);
            } while (curr == COMMA);
            if (curr != CL_PT_BRACE) scanner.fail(">");
            curr = scanner.next(tokens);
        }
        if (curr != OP_BRACE) scanner.fail("{");
        curr = scanner.next(tokens);
        while (curr != CL_BRACE) {
            if (curr == BYTE) {
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
        sources.add(mc);
    }

    private void parseAddress(SingleLineScanner scanner, MachineUsable mc) {
        Token curr = scanner.expect(tokens, NAME);
        mc.addresses.add(curr.matched());
        scanner.expect(tokens, SEMI_COLON);
    }

    private void parseVariable(SingleLineScanner scanner, MachineUsable mc) {
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

    private void parseConstructor(SingleLineScanner scanner, MachineUsable mc) {
        Method mcc = new Method();
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
            if (scanner.peek(tokens) == COMMA) scanner.next(tokens);
            curr = scanner.next(tokens);
        }
        scanner.expect(tokens, OP_BRACE);
        scanner.next(tokens);
        parseStatements(scanner, mcc.body);
        mc.constructors.add(mcc);
    }

    private void parseMethod(SingleLineScanner scanner, MachineUsable mc) {
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
            if (scanner.peek(tokens) == COMMA) scanner.next(tokens);
            curr = scanner.next(tokens);
        }
        scanner.expect(tokens, OP_BRACE);
        scanner.next(tokens);
        parseStatements(scanner, mcm.body);
        mc.methods.add(mcm);
    }

    void parseStatements(SingleLineScanner scanner, List<Statement> statements) {
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
                Statement statement;
                List<String> arguments;
                switch (name) {
                    case "Admin": {
                        AdminStatement is = new AdminStatement();
                        arguments = is.arguments;
                        statement = is;
                        break;
                    }
                    case "Context": {
                        ContextStatement is = new ContextStatement();
                        arguments = is.arguments;
                        statement = is;
                        break;
                    }
                    case "Symbol": {
                        SymbolStatement is = new SymbolStatement();
                        arguments = is.arguments;
                        statement = is;
                        break;
                    }
                    case "Proxy": {
                        ProxyStatement is = new ProxyStatement();
                        arguments = is.arguments;
                        statement = is;
                        break;
                    }
                    case "Direction": {
                        DirectionStatement s = new DirectionStatement();
                        arguments = s.arguments;
                        statement = s;
                        break;
                    }
                    default: {
                        InstructionStatement is = new InstructionStatement(name);
                        arguments = is.arguments;
                        statement = is;
                        break;
                    }
                }
                scanner.expect(tokens, OP_PAREN);
                curr = scanner.next(tokens);
                while (curr != CL_PAREN) {
                    if (curr == LITERAL_ARG) {
                        arguments.add(curr.matched());
                    } else if (curr == NAME) {
                        String fullVal = "";
                        while (curr == NAME) {
                            fullVal += curr.matched();
                            if (scanner.peek(tokens) != DOT) break;
                            scanner.next(tokens);
                            curr = scanner.expect(tokens, NAME);
                            fullVal += ".";
                        }
                        arguments.add(fullVal);
                    }
                    if (scanner.peek(tokens) == COMMA) scanner.next(tokens);
                    curr = scanner.next(tokens);
                }
                statements.add(statement);
            } else if (curr == BYTE) {
                DataStatement ds = new DataStatement();
                curr = scanner.next(tokens);
                while (curr != NAME) {
                    if (curr != OP_SQ_BRACKET) scanner.fail("[");
                    curr = scanner.next(tokens);
                    if (curr == NUMBER) {
                        ds.sizes.add("IL");
                    } else {
                        ds.sizes.add(curr.matched());
                        curr = scanner.next(tokens);
                        if (curr != LITERAL_ARG  && curr != NAME) scanner.fail("number or name");
                    }
                    ds.sizes.add(curr.matched());
                    scanner.expect(tokens, CL_SQ_BRACKET);
                    curr = scanner.next(tokens);
                }
                ds.name = curr.matched();
                statements.add(ds);
            } else if (curr == POSITION) {
                PositionStatement ps = new PositionStatement();
                curr = scanner.expect(tokens, NAME);
                ps.addr = curr.matched();
                Token peek = scanner.peek(tokens);
                if (peek == NAME) {
                    curr = scanner.next(tokens);
                    ps.prevName = curr.matched();
                }
                statements.add(ps);
            } else if (curr == BLOCK) {
                BlockStatement b = new BlockStatement();
                curr = scanner.expect(tokens, NAME);
                b.name = curr.matched();
                Token peek = scanner.peek(tokens);
                if (peek == NAME) {
                    curr = scanner.next(tokens);
                    if (!curr.matched().equals("default")) scanner.fail("default or {");
                    peek = scanner.peek(tokens);
                    if (peek != OP_BRACE) scanner.fail("{");
                } else if (peek == OP_BRACE) {
                    b.isContextPush = true;
                }
                if (peek == OP_BRACE) {
                    scanner.next(tokens);
                    scanner.next(tokens);
                    parseStatements(scanner, b.block);
                }
                statements.add(b);
            } else {
                scanner.fail("Bad statement");
            }
            scanner.expect(tokens, SEMI_COLON);
            curr = scanner.next(tokens);
        }
    }

}
