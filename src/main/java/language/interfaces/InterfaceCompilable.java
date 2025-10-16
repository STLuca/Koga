package language.interfaces;

import core.Types;
import language.core.Sources;
import language.core.Compilable;
import language.core.Compiler;

import java.util.ArrayList;
import java.util.List;

public class InterfaceCompilable implements Compilable {

    String name;
    ArrayList<String> imports = new ArrayList<>();
    ArrayList<String> dependencies = new ArrayList<>();
    ArrayList<Method> methods = new ArrayList<>();

    
    public String name() {
        return name;
    }
    
    public List<String> dependencies() {
        return dependencies;
    }
    
    public void compile(Sources sources, Compiler compiler, Level level) {
        compiler.name(name);
        compiler.type(Types.Document.Interface);
//        int classSymbol = compiler.symbol(Class.Symbol.Type.CLASS, name);
//        for (Method method : methods) {
//            compiler.symbol(Class.Symbol.Type.METHOD, classSymbol, method.name);
//        }
        for (Method method : methods) {
            Compiler.MethodCompiler mb = compiler.method();
            mb.name(method.name);
        }
    }
}
