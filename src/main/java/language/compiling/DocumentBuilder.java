package language.compiling;

import core.Document;
import core.Types;
import language.core.Compiler;

import java.util.*;

public class DocumentBuilder implements Compiler {

    record Data(int start, int size) {}
    record Symbol(Types.Symbol type, String one, String two) {}
    record Constant(String name, byte[] bytes) {}

    String name;
    Types.Document type;
    String administrator;
    LinkedHashSet<String> dependencies = new LinkedHashSet<>();
    ArrayList<Constant> constants = new ArrayList<>();
    ArrayList<Symbol> symbols = new ArrayList<>();
    ArrayList<String> implementing = new ArrayList<>();
    ArrayList<String> supporting = new ArrayList<>();
    int currData = 0;
    HashMap<String, Data> data = new HashMap<>();
    ArrayList<MethodBuilder> methodBuilders = new ArrayList<>();

    ProtocolBuilder protocolBuilder;
    
    public Document document() {
        if (protocolBuilder != null) {
            Document c = protocolBuilder.document();
            c.name = name;
            return c;
        }

        Document d = new Document();
        d.type = type;
        d.name = name;
        d.administrator = administrator;
        d.dependencies = dependencies.toArray(new String[0]);
        d.implementing = implementing.toArray(new String[0]);
        d.supporting = supporting.toArray(new String[0]);
        d.consts = new Document.Const[constants.size()];
        for (int i = 0; i < constants.size(); i++) {
            Constant constant = constants.get(i);
            Document.Const con = new Document.Const();
            con.name = constant.name;
            con.value = constant.bytes;
            d.consts[i] = con;
        }
        d.symbols = new Document.Symbol[symbols.size()];
        for (int i = 0; i < symbols.size(); i++) {
            Symbol symbol = symbols.get(i);
            Document.Symbol s = new Document.Symbol();
            s.type = symbol.type;
            if (symbol.two == null) {
                s.identifier = symbol.one;
            } else {
                s.identifier = symbol.one + " " + symbol.two;
            }
            d.symbols[i] = s;
        }

        ArrayList<Map.Entry<String, Data>> entries = new ArrayList<>(data.entrySet());
        d.data = new Document.Data[entries.size()];
        for (int i = 0; i < entries.size(); i++) {
            Map.Entry<String, Data> e = entries.get(i);
            d.data[i] = new Document.Data(e.getKey(), e.getValue().start, e.getValue().size);
        }

        d.size = currData;

        d.methods = new Document.Method[methodBuilders.size()];
        for (int i = 0; i < methodBuilders.size(); i++) {
            MethodBuilder mb = methodBuilders.get(i);
            d.methods[i] = mb.method();
        }
        return d;
    }
    
    public void name(String name) {
        this.name = name;
    }
    
    public void type(Types.Document type) {
        this.type = type;
    }

    public ProtocolCompiler protocol() {
        protocolBuilder = new ProtocolBuilder();
        return protocolBuilder;
    }

    public MethodCompiler method() {
        MethodBuilder mb = new MethodBuilder(this);
        methodBuilders.add(mb);
        return mb;
    }

    public void administrator(String name) {
        administrator = name;
    }

    public void dependency(String name) {
        dependencies.add(name);
    }

    public int symbol(Types.Symbol type, String name) {
        Symbol contains = null;
        for (Symbol s : symbols) {
            if (s.type == type && s.one.equals(name) && s.two == null) {
                contains = s;
                break;
            }
        }
        if (contains != null) {
            return symbols.indexOf(contains);
        }
        Symbol newSymbol = new Symbol(type, name, null);
        symbols.add(newSymbol);
        return symbols.size() - 1;
    }
    
    public int symbol(Types.Symbol type, String document, String name) {
        Symbol contains = null;
        for (Symbol s : symbols) {
            if (s.type == type && s.one.equals(document) && s.two.equals(name)) {
                contains = s;
                break;
            }
        }
        if (contains != null) {
            return symbols.indexOf(contains);
        }
        Symbol newSymbol = new Symbol(type, document, name);
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

    public void supporting(String protocol) {
        supporting.add(protocol);
    }

    public int data(String name, int size) {
        int location = currData;
        data.put(name, new Data(currData, size));
        currData+=size;
        return location;
    }
}
