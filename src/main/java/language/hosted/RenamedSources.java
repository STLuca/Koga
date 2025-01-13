package language.hosted;

import core.Document;
import language.core.Sources;
import language.core.Compilable;
import language.core.Usable;

import java.util.HashMap;

public class RenamedSources implements Sources {

    Sources root;
    HashMap<String, Usable> usables = new HashMap<>();
    HashMap<String, Compilable> compilables = new HashMap<>();
    HashMap<String, Document> documents = new HashMap<>();

    public RenamedSources(Sources root) {
        this.root = root;
    }

    public Sources root() {
        return root;
    }

    public boolean parse(String name) {
        return false;
    }

    public Usable usable(String name) {
        return usables.get(name);
    }

    public Document document(String name) {
        return documents.get(name);
    }

    public Compilable compilable(String name) {
        return compilables.get(name);
    }

    public void add(Usable c) {
        throw new UnsupportedOperationException();
    }

    public void add(Document d) {
        throw new UnsupportedOperationException();
    }

    public void add(Compilable c) {
        throw new UnsupportedOperationException();
    }
}
