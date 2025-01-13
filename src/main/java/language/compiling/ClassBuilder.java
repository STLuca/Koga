package language.compiling;

import core.Class;
import language.core.Compiler;

import java.util.*;

public class ClassBuilder implements Compiler {

    record Data(int start, int size) {}
    record Symbol(Class.Symbol.Type type, String one, String two) {}
    record Constant(String name, byte[] bytes) {}

    String name;
    Class.Type type;
    List<Constant> constants = new ArrayList<>();
    List<Symbol> symbols = new ArrayList<>();
    List<String> implementing = new ArrayList<>();
    int currData = 0;
    Map<String, Data> data = new HashMap<>();
    List<MethodBuilder> methodBuilders = new ArrayList<>();

    
    public Class clazz() {
        Class c = new Class();
        c.type = type;
        c.name = name;
        c.implementing.addAll(implementing);
        for (Constant constant : constants) {
            Class.Const con = new Class.Const();
            con.name = constant.name;
            con.value = constant.bytes;
            c.consts.add(con);
        }
        for (Symbol symbol : symbols) {
            Class.Symbol s = new Class.Symbol();
            s.type = symbol.type;
            if (symbol.two == null) {
                s.identifier = symbol.one;
            } else {
                s.identifier = symbol.one + "." + symbol.two;
            }
            c.symbols.add(s);
        }

        for (Map.Entry<String, Data> e : data.entrySet()) {
            c.data.add(new Class.Data(e.getKey(), e.getValue().start, e.getValue().size));
        }
        c.size = currData;
        for (MethodBuilder mb : methodBuilders) {
            c.methods.add(mb.method());
        }
        return c;
    }

    
    public void clazz(String name) {
        this.name = name;
    }

    
    public void type(Class.Type type) {
        this.type = type;
    }

    
    public MethodCompiler method() {
        MethodBuilder mb = new MethodBuilder(this);
        methodBuilders.add(mb);
        return mb;
    }

    
    public int symbol(Class.Symbol.Type type, String name) {
        Symbol contains = symbols.stream()
                .filter(s -> s.type == type && s.one.equals(name) && s.two == null)
                .findFirst()
                .orElse(null);
        if (contains != null) {
            return symbols.indexOf(contains);
        }
        Symbol newSymbol = new Symbol(type, name, null);
        symbols.add(newSymbol);
        return symbols.size() - 1;
    }

    
    public int symbol(Class.Symbol.Type type, String clazz, String name) {
        Symbol contains = symbols.stream()
                .filter(s -> s.type == type && s.one.equals(clazz) && s.two.equals(name))
                .findFirst()
                .orElse(null);
        if (contains != null) {
            return symbols.indexOf(contains);
        }
        Symbol newSymbol = new Symbol(type, clazz, name);
        symbols.add(newSymbol);
        return symbols.size() - 1;
    }

    
    public void constant(String name, byte[] bytes) {
        Constant constant = new Constant(name, bytes);
        int index = constants.size();
        constants.add(constant);
    }

    
    public void implementing(String name) {
        implementing.add(name);
    }

    
    public int data(String name, int size) {
        int location = currData;
        data.put(name, new Data(currData, size));
        currData+=size;
        return location;
    }
}
