package language.compiling;

import core.Document;
import core.Types;
import language.core.Compiler;

import java.util.ArrayList;

public class ProtocolBuilder implements Compiler.ProtocolCompiler {

    ArrayList<ProtocolMethodBuilder> builders = new ArrayList<>();
    
    public Document document() {
        Document c = new Document();
        c.type = Types.Document.Protocol;
        c.protocolMethods = new Document.ProtocolMethod[builders.size()];
        for (int i = 0; i < builders.size(); i++) {
            ProtocolMethodBuilder pmb = builders.get(i);
            c.protocolMethods[i] = pmb.method();
        }
        return c;
    }
    
    public Compiler.ProtocolMethodCompiler method() {
        ProtocolMethodBuilder mb = new ProtocolMethodBuilder();
        builders.add(mb);
        return mb;
    }

    public static class ProtocolMethodBuilder implements Compiler.ProtocolMethodCompiler {

        String name;
        ArrayList<Document.Port> ports = new ArrayList<>();
        
        public Document.ProtocolMethod method() {
            Document.ProtocolMethod pm = new Document.ProtocolMethod();
            pm.name = name;
            pm.ports = ports.toArray(new Document.Port[0]);
            return pm;
        }
        
        public void name(String name) {
            this.name = name;
        }
        
        public void param(int size, int permission) {
            ports.add(new Document.Port(size, permission));
        }
    }

}
