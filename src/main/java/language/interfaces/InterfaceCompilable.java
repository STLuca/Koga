package language.interfaces;

import core.Class;
import language.core.Classes;
import language.core.Compilable;
import language.core.Compiler;

import java.util.ArrayList;
import java.util.List;

public class InterfaceCompilable implements Compilable {

    String name;
    List<String> imports = new ArrayList<>();
    List<String> dependencies = new ArrayList<>();
    List<Method> methods = new ArrayList<>();

    
    public String name() {
        return name;
    }

    
    public List<String> dependencies() {
        return dependencies;
    }

    
    public void compile(Classes classes, Compiler compiler) {
        compiler.clazz(name);
        compiler.type(Class.Type.Interface);
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
