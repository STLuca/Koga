package language.system;

import language.core.Classes;
import language.core.Parser;
import language.scanning.Scanner;
import language.scanning.Token;
import language.scanning.Tokens;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SystemParser implements Parser {

    Token DEPENDENCIES, CONSTANTS, IMPORTS, CONSTRUCTOR, NAME, OP_BRACE, CL_BRACE, OP_PAREN, CL_PAREN,
            SEMI_COLON, NUMBER, PARAM_TYPE, COMMA, LITERAL_ARG, OP_SQ_BRACKET, COMPONENTS,
            CL_SQ_BRACKET, EQUALS, DOT, OP_PT_BRACE, CL_PT_BRACE, STRING, OPERATOR, TILDA;
    Tokens tokens = new Tokens();
    Tokens methodNameTokens = new Tokens();
    Map<String, String> operatorNames = new HashMap<>();

    {
        DEPENDENCIES  = tokens.add("'dependencies'");
        IMPORTS       = tokens.add("'imports'");
        CONSTANTS     = tokens.add("'constants'");
        CONSTRUCTOR   = tokens.add("'constructor'");
        COMPONENTS    = tokens.add("'components'");
        PARAM_TYPE    = tokens.add("b[1-9][0-9]*");
        LITERAL_ARG   = tokens.add("0b[0-1]+|0x[0-9a-f]+|[0-9]+|true|false|\\'[a-zA-Z]\\'");
        NAME          = tokens.add("[a-zA-Z]+");
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
        TILDA         = tokens.add("'~'");
        STRING        = tokens.add("\".*\"");

        methodNameTokens.add(NAME);
        methodNameTokens.add(SEMI_COLON);
        OPERATOR = methodNameTokens.add("[^a-zA-Z1-9({ \t]+");

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
        }
    }

    
    public String name() {
        return "system";
    }

    public void parse(Classes classes, String input) {
        Scanner scanner = new Scanner(tokens, input);
        SystemCompilable c = new SystemCompilable();
        // Token curr = scanner.expect(tokens, DISTINGUISH, "Expecting Distinguish");
        Token curr = scanner.current();

        if (curr == IMPORTS) {
            scanner.expect(tokens, OP_BRACE);
            curr = scanner.next(tokens);
            while (curr != CL_BRACE) {
                if (curr != NAME) throw new RuntimeException("name");
                c.imports.add(curr.matched());
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
                c.dependencies.add(curr.matched());
                scanner.expect(tokens, SEMI_COLON);
                curr = scanner.next(tokens);
            }
            curr = scanner.next(tokens);
        }
        if (curr == CONSTANTS) {
            scanner.expect(tokens, OP_BRACE);
            curr = scanner.next(tokens);
            while (curr != CL_BRACE) {
                if (curr != NAME) throw new RuntimeException("name");
                Constant constant = new Constant();
                constant.name = curr.matched();
                constant.literals = new ArrayList<>();
                curr = scanner.next(tokens);
                if (curr == OP_SQ_BRACKET) {
                    constant.type = Constant.Type.Nums;
                    curr = scanner.next(tokens);
                    while (curr != CL_SQ_BRACKET) {
                        if (curr != LITERAL_ARG) scanner.fail("literal");
                        constant.literals.add(curr.matched());
                        curr = scanner.next(tokens);
                    }
                } else if (curr == STRING) {
                    constant.type = Constant.Type.String;
                    curr.matched().chars().forEach(i -> constant.literals.add(String.valueOf(i)));
                    constant.literals.remove(0);
                    constant.literals.remove(constant.literals.size() - 1);
                }
                c.constants.add(constant);
                scanner.expect(tokens, SEMI_COLON);
                curr = scanner.next(tokens);
            }
            curr = scanner.next(tokens);
        }
        if (curr == COMPONENTS) {
            scanner.expect(tokens, OP_BRACE);
            curr = scanner.expect(tokens, NAME);
            if (!curr.matched().equals("allocator")) scanner.fail("allocator");
            curr = scanner.expect(tokens, NAME);
            c.allocator = curr.matched();
            scanner.expect(tokens, SEMI_COLON);
            scanner.expect(tokens, CL_BRACE);
            curr = scanner.next(tokens);
        }
        if (curr != NAME) scanner.fail("name");
        c.name = curr.matched();
        scanner.expect(tokens, OP_BRACE);
        curr = scanner.next(tokens);
        while (curr != CL_BRACE) {
            Token peek = scanner.peek(tokens).orElseThrow();
            if (curr == NAME && peek == OP_PAREN) {
                parseMethod(scanner, c);
            } else if (curr == NAME) {
                parseVariable(scanner, c);
            } else if (curr == TILDA) {
                scanner.takeUntilNewLine();
            }
            curr = scanner.next(tokens);
        }
        classes.add(c);
    }

    private void parseMethod(Scanner scanner, SystemCompilable c) {
        Token curr = scanner.current();
        Method m = new Method();
        m.name = curr.matched();
        scanner.expect(tokens, OP_PAREN);
        curr = scanner.next(tokens);
        while (curr != CL_PAREN) {
            if (curr != NAME) scanner.fail("name");
            Parameter p = new Parameter();
            p.clazz = curr.matched();
            curr = scanner.next(tokens);

            if (curr == OP_PT_BRACE) {
                curr = scanner.next(tokens);
                while (curr != CL_PT_BRACE) {
                    if (curr != NAME) scanner.fail("name");
                    p.generics.add(curr.matched());
                    curr = scanner.next(tokens);
                    if (curr == COMMA) curr = scanner.next(tokens);
                }
                curr = scanner.next(tokens);
            }

            if (curr != NAME) scanner.fail("name");
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
                parseStatement(scanner, c, m.statements);
            }

            curr = scanner.next(tokens);
        }
        c.methods.add(m);
    }

    private void parseStatement(Scanner scanner, SystemCompilable c, List<Statement> statements) {
        Token curr = scanner.current();
        if (curr != NAME) scanner.fail("name");
        String currS = curr.matched();

        // If imports contains the first String, it's a construct statement
        if (c.imports.contains(currS)) {
            Statement s = new Statement();
            // Construct statement
            // check for generics
            // check if there is a name
            // check if 1 arg constructor
            // should be series of ( and {
            // Can continue into invokes if name before ;
            s.type = Statement.Type.CONSTRUCT;
            s.clazz = currS;

            // generics
            if (scanner.peek(tokens).orElse(null) == OP_PT_BRACE) {
                curr = scanner.next(tokens);
                do {
                    curr = scanner.next(tokens);
                    if (curr != NAME) scanner.fail("name");
                    s.generics.add(curr.matched());
                    curr = scanner.next(tokens);
                } while (curr == COMMA);
                if (curr != CL_PT_BRACE) scanner.fail(">");
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
            parseMethodArguments(scanner, c, s);
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
            parseMethodArguments(scanner, c, s);
            statements.add(s);
            curr = scanner.current();
        }
    }

    private void parseMethodArguments(Scanner scanner, SystemCompilable c, Statement s) {
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
                    parseStatement(scanner, c, arg.block);
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

    private void parseVariable(Scanner scanner, SystemCompilable c) {
        Field f = new Field();
        Token curr = scanner.current();
        f.clazz = curr.matched();
        curr = scanner.next(tokens);
        if (curr == OP_PT_BRACE) {
            do {
                curr = scanner.expect(tokens, NAME);
                f.generics.add(curr.matched());
                curr = scanner.next(tokens);
            } while (curr == COMMA);
            if (curr != CL_PT_BRACE) scanner.fail(">");
            curr = scanner.next(tokens);
        }
        if (curr != NAME) scanner.fail("field name");
        f.name = curr.matched();
        scanner.expect(tokens, SEMI_COLON);
        c.fields.add(f);
    }


}
