package language.protocol;

import core.Class;
import language.core.Classes;
import language.core.Compilable;
import language.core.Compiler;

import java.util.ArrayList;
import java.util.List;

public class Protocol implements Compilable {

    String name;
    List<Method> methods = new ArrayList<>();
    
    public String name() {
        return name;
    }
    
    public List<String> dependencies() {
        return List.of();
    }
    
    public void compile(Classes classes, Compiler compiler) {
        compiler.clazz(name);
        compiler.type(Class.Type.Protocol);

        for (Method m : methods) {
            Compiler.MethodCompiler mb = compiler.method();
            mb.name(m.name);
        }
    }

}
