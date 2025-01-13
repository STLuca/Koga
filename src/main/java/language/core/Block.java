package language.core;

public interface Block {
    // List<Variable> implicits?
    void execute(Compiler.MethodCompiler compiler);
}
