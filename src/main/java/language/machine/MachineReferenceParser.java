package language.machine;

import language.core.Parser;
import language.core.Structure;
import language.scanning.Scanner;
import language.scanning.Token;
import language.scanning.Tokens;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MachineReferenceParser implements Parser {

    static class Context {
        MachineReferenceStructure c;
        HashMap<String, String> structures = new HashMap<>();
        ArrayList<String> generics = new ArrayList<>();
    }

    Token CONSTRUCTOR, NAME, GLOBAL_NAME, IMPORTS, BYTE, ADDR, POSITION, BLOCK,
          NUMBER, LITERAL_ARG, PARAM_TYPE, PARAM_ARRAY,
          OP_BRACE, CL_BRACE, OP_PAREN, CL_PAREN, OP_SQ_BRACKET, CL_SQ_BRACKET, OP_PT_BRACE, CL_PT_BRACE,
          SEMI_COLON, COMMA, DOT, TILDA;
    Tokens tokens = new Tokens();
    Tokens paramTokens = new Tokens();
    Tokens metaTokens = new Tokens();

    {
        CONSTRUCTOR   = tokens.add("'constructor'");
        BYTE          = tokens.add("'Byte'");
        ADDR          = tokens.add("'Addr'");
        POSITION      = tokens.add("'Position'");
        BLOCK         = tokens.add("'Block'");
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

        PARAM_TYPE    = paramTokens.add("b[1-9][0-9]*");
        PARAM_ARRAY   = paramTokens.add("\\[\\]");
        paramTokens.add(COMMA);
        paramTokens.add(CL_PAREN);
        paramTokens.add(OP_PAREN);
        paramTokens.add(OP_PT_BRACE);
        paramTokens.add(CL_PT_BRACE);
        paramTokens.add(NAME);

        IMPORTS     = metaTokens.add("'structures'");
        metaTokens.add(SEMI_COLON);
        metaTokens.add(OP_BRACE);
        metaTokens.add(CL_BRACE);
        GLOBAL_NAME = metaTokens.add("[a-zA-Z]+\\.[a-zA-Z]+(\\.[a-zA-Z]+)*");
        metaTokens.add(NAME);
    }
    
    public String name() {
        return "machineReference";
    }
    
    public Output parse(String input) {
        Scanner scanner = new Scanner(input);
        MachineReferenceStructure mc = new MachineReferenceStructure();
        Context ctx = new Context();
        ctx.c = mc;
        Token curr = scanner.next(metaTokens);

        if (curr == IMPORTS) {
            scanner.expect(metaTokens, OP_BRACE);
            curr = scanner.next(metaTokens);
            while (curr != CL_BRACE) {
                if (curr != GLOBAL_NAME) scanner.fail("Global name");
                String global = curr.matched();
                String[] split = global.split("\\.");
                String local = split[split.length - 1];
                curr = scanner.next(metaTokens);
                if (curr == NAME) {
                    local = curr.matched();
                    curr = scanner.next(metaTokens);
                }
                ctx.structures.put(local, global);
                if (curr != SEMI_COLON) scanner.fail(";");
                curr = scanner.next(metaTokens);
            }
            curr = scanner.next(metaTokens);
        }

        if (curr != GLOBAL_NAME) scanner.fail("name");
        mc.name = curr.matched();

        String[] splitName = mc.name.split("\\.");
        ctx.structures.put(splitName[splitName.length - 1], mc.name);

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
                ctx.generics.add(name);
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
                parseConstructor(scanner, ctx);
            } else if (curr == NAME) {
                parseMethod(scanner, ctx);
            } else if (curr == TILDA) {
                scanner.takeUntilNewLine();
            }
            curr = scanner.next(tokens);
        }

        Output out = new Output();
        out.names = new String[] { mc.name };
        out.structures = new Structure[] { mc };
        return out;
    }

    private void parseAddress(Scanner scanner, MachineReferenceStructure mc) {
        Token curr = scanner.expect(tokens, NAME);
        mc.addresses.add(curr.matched());
        scanner.expect(tokens, SEMI_COLON);
    }

    private void parseVariable(Scanner scanner, MachineReferenceStructure mc) {
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

    private void parseConstructor(Scanner scanner, Context ctx) {
        Operation mcc = new Operation();
        Token curr = scanner.next(tokens);
        if (curr == NAME) {
            mcc.name = curr.matched();
            scanner.expect(tokens, OP_PAREN);
        } else if (curr == OP_PAREN) {
            mcc.name = "";
        } else {
            scanner.fail("(");
        }
        parseParameters(scanner, ctx, mcc.parameters);
        scanner.expect(tokens, OP_BRACE);
        curr = scanner.next(tokens);
        parseBody(scanner, ctx.c, mcc, mcc.body);
        ctx.c.constructors.add(mcc);
    }

    private void parseMethod(Scanner scanner, Context ctx) {
        Operation mcm = new Operation();
        Token curr = scanner.current();
        if (curr != NAME) scanner.fail("name");
        mcm.name = curr.matched();
        scanner.expect(tokens, OP_PAREN);
        parseParameters(scanner, ctx, mcm.parameters);
        scanner.expect(tokens, OP_BRACE);
        curr = scanner.next(tokens);
        parseBody(scanner, ctx.c, mcm, mcm.body);
        if (mcm.name.equals("invoke")) {
            ctx.c.invokeOperation = mcm;
        } else if (mcm.name.equals("arg")) {
            ctx.c.argOperation = mcm;
        }
    }

    void parseParameters(Scanner scanner, Context ctx, ArrayList<Operation.Parameter> parameters) {
        Token curr = scanner.next(paramTokens);
        while (curr != CL_PAREN) {
            if (curr != PARAM_TYPE && curr != NAME) throw new RuntimeException("expecting parameter type");
            boolean binaryType = curr == PARAM_TYPE;
            String paramType = curr.matched();
            if (binaryType) {
                curr = scanner.next(paramTokens);
                boolean isArray = false;
                if (curr == PARAM_ARRAY) {
                    isArray = true;
                    curr = scanner.next(paramTokens);
                }
                Operation.Parameter p = new Operation.Parameter();
                p.type = Operation.Parameter.Type.Literal;
                p.bits = Integer.parseInt(paramType.substring(1));
                p.array = isArray;
                p.name = curr.matched();
                parameters.add(p);
            } else {
                Operation.Parameter p = new Operation.Parameter();
                if (paramType.equals("Block")) {
                    p.type = Operation.Parameter.Type.Block;
                    curr = scanner.next(tokens);
                } else if (paramType.equals("Name")) {
                    p.type = Operation.Parameter.Type.Name;
                    curr = scanner.next(tokens);
                } else {
                    p.type = Operation.Parameter.Type.Variable;
                    Operation.Descriptor rootDescriptor = new Operation.Descriptor();
                    p.descriptor = rootDescriptor;
                    ArrayDeque<Operation.Descriptor> stack = new ArrayDeque<>();
                    stack.push(rootDescriptor);

                    while (!stack.isEmpty()) {
                        if (curr == NAME) {
                            Operation.Descriptor d = stack.peek();
                            if (d.name != null) { break; }
                            if (ctx.structures.containsKey(curr.matched())) {
                                d.type = Operation.Descriptor.Type.Structure;
                                d.name = ctx.structures.get(curr.matched());
                            } else if (ctx.generics.contains(curr.matched())) {
                                d.type = Operation.Descriptor.Type.Generic;
                                d.name = curr.matched();
                            } else if (curr.matched().equals("Variable")) {
                                d.type = Operation.Descriptor.Type.Structure;
                                d.name = "Variable";
                            } else {
                                scanner.fail("");
                            }
                        } else if (curr == OP_PT_BRACE) {
                            Operation.Descriptor d = stack.peek();
                            Operation.Descriptor subDescriptor = new Operation.Descriptor();
                            d.subDescriptors.add(subDescriptor);
                            stack.push(subDescriptor);
                        } else if (curr == CL_PT_BRACE) {
                            stack.pop();
                            if (stack.peek() == rootDescriptor) {
                                stack.pop();
                            }
                        } else if (curr == COMMA) {
                            stack.pop();
                            Operation.Descriptor d = stack.peek();
                            Operation.Descriptor subDescriptor = new Operation.Descriptor();
                            d.subDescriptors.add(subDescriptor);
                            stack.push(subDescriptor);
                        }
                        curr = scanner.next(tokens);
                    }
                }
                p.array = false;
                if (curr != NAME) {
                    scanner.fail("name");
                }
                p.name = curr.matched();
                parameters.add(p);
            }
            if (scanner.peek(paramTokens).orElse(null) == COMMA) scanner.next(paramTokens);
            curr = scanner.next(paramTokens);
        }
    }

    private void parseBody(Scanner scanner, MachineReferenceStructure mc, Operation operation, List<Statement> body) {
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
                operation.addresses.add(as.name);
            } else if (curr == NAME) {
                String name = curr.matched();
                Statement statement;
                List<String> arguments;
                switch (name) {
                    case "Args" -> {
                        statement = new MachineReferenceStructure.ArgsStatement();
                        arguments = new ArrayList<>();
                        mc.argStatement = statement;
                    }
                    case "ArgsCopy" -> {
                        MachineReferenceStructure.ArgsCopyStatement is = new MachineReferenceStructure.ArgsCopyStatement();
                        arguments = is.arguments;
                        statement = is;
                    }
                    case "Admin" -> {
                        AdminStatement is = new AdminStatement();
                        arguments = is.arguments;
                        statement = is;
                    }
                    case "Context" -> {
                        ContextStatement is = new ContextStatement();
                        arguments = is.arguments;
                        statement = is;
                    }
                    case "Constant" -> {
                        ConstantStatement is = new ConstantStatement();
                        arguments = is.arguments;
                        statement = is;
                    }
                    case "Symbol" -> {
                        SymbolStatement is = new SymbolStatement();
                        arguments = is.arguments;
                        statement = is;
                    }
                    case "Proxy" -> {
                        ProxyStatement is = new ProxyStatement();
                        arguments = is.arguments;
                        statement = is;
                    }
                    case "Direction" -> {
                        DirectionStatement s = new DirectionStatement();
                        arguments = s.arguments;
                        statement = s;
                    }
                    default -> {
                        InstructionStatement is = new InstructionStatement(name);
                        arguments = is.arguments;
                        statement = is;
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
                            if (scanner.peek(tokens).orElse(null) != DOT) break;
                            scanner.next(tokens);
                            curr = scanner.expect(tokens, NAME);
                            fullVal += ".";
                        }
                        arguments.add(fullVal);
                    }
                    if (scanner.peek(tokens).orElse(null) == COMMA) scanner.next(tokens);
                    curr = scanner.next(tokens);
                }
                body.add(statement);
            } else if (curr == BYTE) {
                DataStatement ds = new DataStatement();
                curr = scanner.next(tokens);
                while (curr != NAME) {
                    if (curr != OP_SQ_BRACKET) scanner.fail("[");
                    curr = scanner.next(tokens);
                    if (curr == NUMBER) {
                        ds.sizes.add("I");
                    } else {
                        ds.sizes.add(curr.matched());
                        curr = scanner.next(tokens);
                        if (curr != LITERAL_ARG && curr != NAME) scanner.fail("number or name");
                    }
                    ds.sizes.add(curr.matched());
                    scanner.expect(tokens, CL_SQ_BRACKET);
                    curr = scanner.next(tokens);
                }
                ds.name = curr.matched();
                body.add(ds);
            } else if (curr == POSITION) {
                PositionStatement ps = new PositionStatement();
                curr = scanner.expect(tokens, NAME);
                ps.addr = curr.matched();
                Token peek = scanner.peek(tokens).orElse(null);
                if (peek == NAME) {
                    curr = scanner.next(tokens);
                    ps.prevName = curr.matched();
                }
                body.add(ps);
            } else if (curr == BLOCK) {
                BlockStatement b = new BlockStatement();
                curr = scanner.expect(tokens, NAME);
                b.name = curr.matched();
                Token peek = scanner.peek(tokens).orElseThrow();
                if (peek == NAME) {
                    curr = scanner.next(tokens);
                    if (!curr.matched().equals("default")) scanner.fail("default or {");
                    peek = scanner.peek(tokens).orElseThrow();
                    if (peek != OP_BRACE) scanner.fail("{");
                } else if (peek == OP_BRACE) {
                    b.isContextPush = true;
                }
                if (peek == OP_BRACE) {
                    scanner.next(tokens);
                    scanner.next(tokens);
                    parseBody(scanner, mc, operation, b.block);
                }
                body.add(b);
            } else {
                scanner.fail("Bad statement");
            }
            scanner.expect(tokens, SEMI_COLON);
            curr = scanner.next(tokens);
        }
    }
}
