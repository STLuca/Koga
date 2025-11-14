package language.compiling;

import core.Types;
import language.core.Compiler;

import java.util.ArrayList;

public class ProtocolBuilder implements Compiler.ProtocolCompiler {

    ArrayList<ProtocolMethodBuilder> builders = new ArrayList<>();
    
    DocumentStruct document() {
        DocumentStruct c = new DocumentStruct();
        c.type = Types.Document.Protocol;
        c.protocolMethods = new DocumentStruct.ProtocolMethod[builders.size()];
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
        ArrayList<DocumentStruct.Port> ports = new ArrayList<>();
        
        DocumentStruct.ProtocolMethod method() {
            DocumentStruct.ProtocolMethod pm = new DocumentStruct.ProtocolMethod();
            pm.name = name;
            pm.ports = ports.toArray(new DocumentStruct.Port[0]);
            return pm;
        }
        
        public void name(String name) {
            this.name = name;
        }
        
        public void param(int size, int permission) {
            ports.add(new DocumentStruct.Port(size, permission));
        }
    }

}
