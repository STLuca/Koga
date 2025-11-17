package language.protocol;

import language.core.Document;
import language.core.Repository;
import language.core.Compilable;
import language.core.Compiler;

import java.util.ArrayList;
import java.util.List;

public class Protocol implements Compilable {

    String name;
    ArrayList<Method> methods = new ArrayList<>();
    
    public String name() {
        return name;
    }
    
    public List<String> dependencies() {
        return List.of();
    }

    @Override
    public Document document() {
        ProtocolDocument doc = new ProtocolDocument();
        doc.protocol = this;
        return doc;
    }

    public void compile(Repository repository, Compiler compiler) {
        compiler.name(name);
        Compiler.ProtocolCompiler pc = compiler.protocol();

        for (Method m : methods) {
            Compiler.ProtocolMethodCompiler pmc = pc.method();
            pmc.name(m.name);
            for (Method.Parameter parameter : m.parameters) {
                int size = Integer.parseInt(parameter.size);
                pmc.param(size, parameter.permission.ordinal());
            }
        }
    }

}
