package language.protocol;

import language.core.Sources;
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
    
    public void compile(Sources sources, Compiler compiler, Level level) {
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
