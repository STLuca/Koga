package language.hosted;

import language.core.Compilable;
import language.core.Parser;
import language.core.Structure;
import language.scanning.Scanner;
import language.scanning.Token;
import language.scanning.Tokens;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class HostedParser implements Parser {

    static class Context {
        HostedCompilable c;
        HashMap<String, String> structures = new HashMap<>();
        HashMap<String, String> documents = new HashMap<>();
        ArrayList<String> generics = new ArrayList<>();
    }

    Token   DEPENDENCIES, CONSTANTS, IMPORTS, IMPLEMENTS,
            NAME, GLOBAL_NAME, STRING, LITERAL_ARG, OPERATOR,
            SEMI_COLON, COMMA, TILDA,
            OP_BRACE, CL_BRACE, OP_PAREN, CL_PAREN, CL_SQ_BRACKET, OP_SQ_BRACKET, OP_PT_BRACE, CL_PT_BRACE;
    Tokens tokens = new Tokens();
    Tokens methodNameTokens = new Tokens();
    Tokens metaTokens = new Tokens();
    HashMap<String, String> operatorNames = new HashMap<>();

    {
        LITERAL_ARG   = tokens.add("0b[0-1]+|0x[0-9a-f]+|[0-9]+|true|false|\\'[a-zA-Z]\\'");
        NAME          = tokens.add("[a-zA-Z]+");
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
        TILDA         = tokens.add("'~'");
        STRING        = tokens.add("\".*\"");

        methodNameTokens.add(NAME);
        methodNameTokens.add(SEMI_COLON);
        OPERATOR = methodNameTokens.add("[^a-zA-Z1-9({ \t]+");

        {
            DEPENDENCIES  = metaTokens.add("'documents'");
            IMPORTS       = metaTokens.add("'structures'");
            // CONSTANTS     = metaTokens.add("'constants'");
            IMPLEMENTS    = metaTokens.add("'implements'");
            GLOBAL_NAME   = metaTokens.add("[a-zA-Z]+\\.[a-zA-Z]+(\\.[a-zA-Z]+)*");
            metaTokens.add(NAME);
            metaTokens.add(SEMI_COLON);
            metaTokens.add(OP_BRACE);
            metaTokens.add(CL_BRACE);
            metaTokens.add(OP_PT_BRACE);
            metaTokens.add(CL_PT_BRACE);
        }

        {
            operatorNames.put("=", "set");
            operatorNames.put("+", "plus");
            operatorNames.put("-", "minus");
            operatorNames.put("*", "multiply");
            operatorNames.put("/", "divide");
            operatorNames.put("%", "modulus");
            operatorNames.put("++", "increment");
            operatorNames.put("--", "decrement");
            operatorNames.put("==", "equalTo");
            operatorNames.put("!=", "notEqualTo");
            operatorNames.put("<", "lessThan");
            operatorNames.put(">", "greaterThan");
            operatorNames.put(">=", "greaterThanOrEqualTo");
            operatorNames.put("<=", "lessThanOrEqualTo");
            operatorNames.put("->", "copyTo");
            operatorNames.put("<-", "copyFrom");
            operatorNames.put("#", "index");
            operatorNames.put("@", "index");
            operatorNames.put("&", "and");
            operatorNames.put("|", "or");
            operatorNames.put("<<", "shiftLeft");
            operatorNames.put(">>", "shiftRight");
            operatorNames.put("<<<", "altShiftLeft");
            operatorNames.put(">>>", "altShiftRight");
            operatorNames.put("?", "ifTrue");
            operatorNames.put("?!", "ifFalse");
        }
    }
    
    public String name() {
        return "hosted";
    }

    public Output parse(String input) {
        Scanner scanner = new Scanner(input);
        HostedCompilable c = new HostedCompilable();
        Context ctx = new Context();
        ctx.c = c;

        Token curr = scanner.next(metaTokens);
        if (curr == IMPORTS) {
            scanner.expect(metaTokens, OP_BRACE);
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
                c.imports.add(globalName);
                curr = scanner.next(metaTokens);
            }
            curr = scanner.next(metaTokens);
        }
        if (curr == DEPENDENCIES) {
            scanner.expect(metaTokens, OP_BRACE);
            curr = scanner.next(metaTokens);
            while (curr != CL_BRACE) {
                if (curr != NAME && curr != GLOBAL_NAME) throw new RuntimeException("name");
                String globalName = curr.matched();
                String localName = curr.matched();
                if (localName.contains(".")) {
                    String[] split = localName.split("\\.");
                    localName = split[split.length - 1];
                }
                curr = scanner.next(metaTokens);
                if (curr == NAME) {
                    localName = curr.matched();
                    curr = scanner.next(metaTokens);
                }
                if (curr != SEMI_COLON) { scanner.fail(";"); }
                ctx.documents.put(localName, globalName);
                c.dependencies.add(globalName);
                curr = scanner.next(metaTokens);
            }
            curr = scanner.next(metaTokens);
        }
        if (curr != NAME && curr != GLOBAL_NAME) scanner.fail("doc name");
        c.name = curr.matched();
        // Add self as dependency
        String[] split = c.name.split("\\.");
        ctx.documents.put(split[split.length - 1], c.name);
        c.dependencies.add(c.name);

        curr = scanner.next(metaTokens);

        if (curr == OP_PT_BRACE) {
            do {
                curr = scanner.expect(metaTokens, NAME);
                String type = curr.matched();
                curr = scanner.expect(metaTokens, NAME);
                String name = curr.matched();
                Generic g = new Generic();
                g.type = Generic.Type.valueOf(type);
                g.name = name;
                c.generics.add(g);
                ctx.generics.add(name);
                curr = scanner.next(metaTokens);
            } while (curr == COMMA);
            if (curr != CL_PT_BRACE) scanner.fail(">");
            curr = scanner.next(metaTokens);
        }

        if (curr == IMPLEMENTS) {
            while (curr != OP_BRACE) {
                curr = scanner.expect(tokens, NAME);
                c.interfaces.add(ctx.documents.get(curr.matched()));
                curr = scanner.next(tokens);
            }
        }
        if (curr != OP_BRACE) scanner.fail("{");
        curr = scanner.next(tokens);
        while (curr != CL_BRACE) {
            Token peek = scanner.peek(tokens).orElseThrow();
            if (curr == NAME && peek == OP_PAREN) {
                parseMethod(scanner, ctx);
            } else if (curr == NAME) {
                parseField(scanner, ctx);
            } else if (curr == TILDA) {
                scanner.takeUntilNewLine();
            }
            curr = scanner.next(tokens);
        }

        Output out = new Output();
        out.compilables = new Compilable[] { c };
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
            Token next = scanner.peek(tokens).orElse(null);

            if (next == OP_PT_BRACE) {
                ArrayDeque<Structure.GenericArgument> stack = new ArrayDeque<>();
                while (!stack.isEmpty() || curr != CL_PT_BRACE) {
                    curr = scanner.next(tokens);
                    if (curr == OP_PT_BRACE) {
                        stack.push(new Structure.GenericArgument());
                    } else if (curr == CL_PT_BRACE) {
                        Structure.GenericArgument popped = stack.pop();
                        if (stack.isEmpty()) {
                            p.generics.add(popped);
                        } else {
                            stack.peek().generics.add(popped);
                        }
                    } else if (curr == COMMA) {
                        Structure.GenericArgument pop = stack.pop();
                        Structure.GenericArgument peek = stack.peek();
                        if (peek == null) {
                            p.generics.add(pop);
                        } else {
                            pop.generics.add(peek);
                        }
                        stack.push(new Structure.GenericArgument());
                    } else if (curr == NAME) {
                        Structure.GenericArgument peek = stack.peek();
                        if (ctx.structures.containsKey(curr.matched())) {
                            peek.name = ctx.structures.get(curr.matched());
                        } else if (ctx.documents.containsKey(curr.matched())) {
                            peek.name = ctx.documents.get(curr.matched());
                        } else if (ctx.generics.contains(curr.matched())) {
                            peek.name = curr.matched();
                        } else {
                            scanner.fail("");
                        }
                    } else {
                        scanner.fail("");
                    }
                }
            }

            curr = scanner.expect(tokens, NAME);
            p.name = curr.matched();

            curr = scanner.next(tokens);
            if (curr == COMMA) curr = scanner.next(tokens);
            m.params.add(p);
        }
        scanner.expect(tokens, OP_BRACE);
        curr = scanner.next(tokens);
        while (curr != CL_BRACE) {
            if (curr == TILDA) {
                scanner.takeUntilNewLine();
            } else {
                parseStatement(scanner, ctx, m.statements);
            }
            curr = scanner.next(tokens);
        }
        ctx.c.methods.add(m);
    }

    private void parseStatement(Scanner scanner, Context ctx, List<Statement> statements) {
        Token curr = scanner.current();
        if (curr == TILDA) {
            scanner.takeUntilNewLine();
            return;
        }
        if (curr != NAME) scanner.fail("name");
        String currS = curr.matched();

        // If imports contains the first String, it's a construct statement
        boolean contains = ctx.structures.containsKey(currS);
        if (contains) {
            Statement s = new Statement();
            // Construct statement
            // check for generics
            // check if there is a name
            // check if 1 arg constructor
            // should be series of ( and {
            // Can continue into invokes if name before ;
            s.type = Statement.Type.CONSTRUCT;
            s.structure = ctx.structures.get(currS);

            // generics
            if (scanner.peek(tokens).orElse(null) == OP_PT_BRACE) {
                ArrayDeque<Structure.GenericArgument> stack = new ArrayDeque<>();
                while (!stack.isEmpty() || curr != CL_PT_BRACE) {
                    curr = scanner.next(tokens);
                    if (curr == OP_PT_BRACE) {
                        stack.push(new Structure.GenericArgument());
                    } else if (curr == CL_PT_BRACE) {
                        Structure.GenericArgument popped = stack.pop();
                        if (stack.isEmpty()) {
                            s.generics.add(popped);
                        } else {
                            stack.peek().generics.add(popped);
                        }
                    } else if (curr == COMMA) {
                        Structure.GenericArgument pop = stack.pop();
                        Structure.GenericArgument peek = stack.peek();
                        if (peek == null) {
                            s.generics.add(pop);
                        } else {
                            pop.generics.add(peek);
                        }
                        stack.push(new Structure.GenericArgument());
                    } else if (curr == NAME) {
                        Structure.GenericArgument peek = stack.peek();
                        if (ctx.structures.containsKey(curr.matched())) {
                            peek.name = ctx.structures.get(curr.matched());
                        } else if (ctx.documents.containsKey(curr.matched())) {
                            peek.name = ctx.documents.get(curr.matched());
                        } else if (ctx.generics.contains(curr.matched())) {
                            peek.name = curr.matched();
                        } else {
                            scanner.fail("");
                        }
                    } else {
                        scanner.fail("");
                    }
                }
            }

            curr = scanner.next(tokens);


            // Int x;
            // Int x 0;
            // Int x (0);
            // Int x port();
            // While w {};
            // While loop{}; // Maybe don't allow this because you can't tell if it's a variable name or constructor name? perhaps While _ loop{}
            // While {}; // When might the be used? If there was a return inside it?

            // Can be name, can also be open paren/open brace. Would want to check for name before open though
            if (curr == NAME) {
                String name = curr.matched();
                Token peek = scanner.peek(tokens).orElseThrow();
                if (peek == SEMI_COLON) {
                    // name is a variable name
                    s.type = Statement.Type.DECLARE;
                    s.variableName = name;
                    s.methodName = "";
                    statements.add(s);
                    scanner.next(tokens);
                    return;
                } else if (peek == NAME) {
                    // name is the variable name
                    s.variableName = name;
                    // peek can still be method name or variable name e.g. Int x y; or Int x port(); While loop {
                    curr = scanner.next(tokens);
                    s.methodName = curr.matched();
                } else if (peek == OP_PAREN || peek == OP_BRACE) {
                    // must be the constructor name?
                    s.variableName = name;
                    s.methodName = "";
                } else if (peek == LITERAL_ARG || peek == STRING) {
                    s.variableName = name;
                    s.methodName = "";
                }
                curr = scanner.next(tokens);
            } else {
                s.variableName = "_";
                s.methodName = "";
                if (curr == SEMI_COLON) {
                    statements.add(s);
                    return;
                }
            }

            // No more names, just args now?
            parseMethodArguments(scanner, ctx, s);
            statements.add(s);

            // chained statements
            curr = scanner.current();
            if (curr == SEMI_COLON) return;
            currS = s.variableName;
        } else {
            curr = scanner.next(methodNameTokens);
        }
        while (curr != SEMI_COLON) {
            Statement s = new Statement();
            s.type = Statement.Type.INVOKE;
            s.variableName = currS;
            if (curr == NAME) {
                s.methodName = curr.matched();
            } else if (curr == OPERATOR) {
                if (!operatorNames.containsKey(curr.matched())) scanner.fail("Operator " + curr.matched() + " doesn't exist");
                s.methodName = operatorNames.get(curr.matched());
            } else {
                scanner.fail("Failed parsing method name");
            }
            scanner.next(tokens);
            parseMethodArguments(scanner, ctx, s);
            statements.add(s);
            curr = scanner.current();
        }
    }

    private void parseMethodArguments(Scanner scanner, Context ctx, Statement s) {
        Token curr = scanner.current();

        if (curr != OP_PAREN && curr != OP_BRACE) {
            // Single argument only
            if (curr == LITERAL_ARG) {
                Statement.Argument arg = new Statement.Argument();
                arg.literal = curr.matched();
                s.arguments.add(arg);
            } else if (curr == NAME) {
                Statement.Argument arg = new Statement.Argument();
                arg.name = curr.matched();
                s.arguments.add(arg);
            } else if (curr == STRING) {
                Statement.Argument arg = new Statement.Argument();
                arg.array = new ArrayList<>();
                curr.matched().chars().forEach(i -> arg.array.add(String.valueOf(i)));
                arg.array.remove(0);
                arg.array.remove(arg.array.size() - 1);
                s.arguments.add(arg);
            }
            scanner.next(methodNameTokens);
            return;
        }

        // multiple arguments
        while (curr == OP_PAREN || curr == OP_BRACE) {

            if (curr == OP_PAREN) {
                // literals or variables
                curr = scanner.next(tokens);
                while (curr != CL_PAREN) {
                    Statement.Argument arg = new Statement.Argument();
                    if (curr == LITERAL_ARG) {
                        arg.literal = curr.matched();
                    } else if (curr == NAME) {
                        arg.name = curr.matched();
                    } else {
                        scanner.fail("unknown arg");
                    }
                    s.arguments.add(arg);

                    curr = scanner.next(tokens);
                    if (curr == COMMA) curr = scanner.next(tokens);
                }
            } else if (curr == OP_BRACE) {
                // block argument
                Statement.Argument arg = new Statement.Argument();
                arg.block = new ArrayList<>();
                curr = scanner.next(tokens);
                while (curr != CL_BRACE) {
                    parseStatement(scanner, ctx, arg.block);
                    curr = scanner.next(tokens);
                }
                s.arguments.add(arg);
            } else {
                scanner.fail("( or {");
            }

            Token peek = scanner.peek(tokens).orElse(null);
            if (peek == OP_PAREN || peek == OP_BRACE) {
                curr = scanner.next(tokens);
            } else {
                curr = scanner.next(methodNameTokens);
            }
        }
    }

    private void parseField(Scanner scanner, Context ctx) {
        Field f = new Field();
        Token curr = scanner.current();
        f.structure = ctx.structures.get(curr.matched());
        Token next = scanner.peek(tokens).orElse(null);
        if (next == OP_PT_BRACE) {
            ArrayDeque<Structure.GenericArgument> stack = new ArrayDeque<>();
            while (!stack.isEmpty() || curr != CL_PT_BRACE) {
                curr = scanner.next(tokens);
                if (curr == OP_PT_BRACE) {
                    stack.push(new Structure.GenericArgument());
                } else if (curr == CL_PT_BRACE) {
                    Structure.GenericArgument popped = stack.pop();
                    if (stack.isEmpty()) {
                        f.generics.add(popped);
                    } else {
                        stack.peek().generics.add(popped);
                    }
                } else if (curr == COMMA) {
                    Structure.GenericArgument pop = stack.pop();
                    Structure.GenericArgument peek = stack.peek();
                    if (peek == null) {
                        f.generics.add(pop);
                    } else {
                        pop.generics.add(peek);
                    }
                    stack.push(new Structure.GenericArgument());
                } else if (curr == NAME) {
                    Structure.GenericArgument peek = stack.peek();
                    if (ctx.structures.containsKey(curr.matched())) {
                        peek.name = ctx.structures.get(curr.matched());
                    } else if (ctx.documents.containsKey(curr.matched())) {
                        peek.name = ctx.documents.get(curr.matched());
                    } else if (ctx.generics.contains(curr.matched())) {
                        peek.name = curr.matched();
                    } else {
                        scanner.fail("");
                    }
                } else {
                    scanner.fail("");
                }
            }
        }
        curr = scanner.expect(tokens, NAME);
        f.name = curr.matched();
        scanner.expect(tokens, SEMI_COLON);
        ctx.c.fields.add(f);
    }

}
