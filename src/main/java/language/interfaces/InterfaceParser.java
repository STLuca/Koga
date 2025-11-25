package language.interfaces;

import language.core.Compilable;
import language.core.Parser;
import language.scanning.Scanner;
import language.scanning.Token;
import language.scanning.Tokens;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;

public class InterfaceParser implements Parser {

    static class Context {
        InterfaceCompilable ic;
        HashMap<String, String> structures = new HashMap<>();
        HashMap<String, String> documents = new HashMap<>();
        ArrayList<String> generics = new ArrayList<>();
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
        metaTokens.add(OP_PT_BRACE);
        metaTokens.add(CL_PT_BRACE);
        metaTokens.add(SEMI_COLON);
        DEPENDENCIES  = metaTokens.add("'documents'");
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
                ic.dependencies.add(globalName);
                curr = scanner.next(metaTokens);
            }
            curr = scanner.next(metaTokens);
        }
        if (curr != GLOBAL_NAME && curr != NAME) {
            scanner.fail("name");
        }
        ic.name = curr.matched();

        curr = scanner.next(tokens);
        if (curr == OP_PT_BRACE) {
            do {
                curr = scanner.expect(metaTokens, NAME);
                String type = curr.matched();
                curr = scanner.expect(metaTokens, NAME);
                String name = curr.matched();
                Generic g = new Generic();
                g.type = Generic.Type.valueOf(type);
                g.name = name;
                ic.generics.add(g);
                ctx.generics.add(name);
                curr = scanner.next(metaTokens);
            } while (curr == COMMA);
            if (curr != CL_PT_BRACE) scanner.fail(">");
            curr = scanner.next(metaTokens);
        }

        if (curr != OP_BRACE) scanner.fail("{");
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
            Descriptor rootDescriptor = new Descriptor();
            p.descriptor = rootDescriptor;
            ArrayDeque<Descriptor> stack = new ArrayDeque<>();
            stack.push(rootDescriptor);

            while (!stack.isEmpty()) {
                if (curr == NAME) {
                    Descriptor d = stack.peek();
                    if (d.name != null) { break; }
                    if (ctx.structures.containsKey(curr.matched())) {
                        d.type = Descriptor.Type.Structure;
                        d.name = ctx.structures.get(curr.matched());
                    } else if (ctx.documents.containsKey(curr.matched())) {
                        d.type = Descriptor.Type.Document;
                        d.name = ctx.documents.get(curr.matched());
                    } else if (ctx.generics.contains(curr.matched())) {
                        d.type = Descriptor.Type.Generic;
                        d.name = curr.matched();
                    } else {
                        scanner.fail("");
                    }
                } else if (curr == OP_PT_BRACE) {
                    Descriptor d = stack.peek();
                    Descriptor subDescriptor = new Descriptor();
                    d.subDescriptors.add(subDescriptor);
                    stack.push(subDescriptor);
                } else if (curr == CL_PT_BRACE) {
                    stack.pop();
                    if (stack.peek() == rootDescriptor) {
                        stack.pop();
                    }
                } else if (curr == COMMA) {
                    stack.pop();
                    Descriptor d = stack.peek();
                    Descriptor subDescriptor = new Descriptor();
                    d.subDescriptors.add(subDescriptor);
                    stack.push(subDescriptor);
                }
                curr = scanner.next(tokens);
            }
            if (curr != NAME) {
                scanner.fail("");
            }
            p.name = curr.matched();
            m.params.add(p);
            curr = scanner.next(tokens);
            if (curr == COMMA) curr = scanner.next(tokens);
        }
        scanner.expect(tokens, SEMI_COLON);
        ctx.ic.methods.add(m);
    }
}
