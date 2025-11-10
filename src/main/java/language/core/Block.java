package language.core;

public interface Block {

    void execute(Compiler.MethodCompiler compiler, Scope scope);

}
