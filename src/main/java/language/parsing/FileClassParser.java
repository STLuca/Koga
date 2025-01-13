package language.parsing;

import core.Document;
import language.core.Compilable;
import language.core.Parser;
import language.core.Sources;
import language.core.Usable;
import language.machine.MachineProxyUsable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileClassParser implements Sources {

    Path root;
    HashMap<String, String> files;
    HashSet<String> parsed = new HashSet<>();

    LinkedHashMap<String, Parser> parsers = new LinkedHashMap<>();

    HashMap<String, Compilable> compilableClasses = new HashMap<>();
    HashMap<String, Usable> compilerClasses = new HashMap<>();
    HashMap<String, Document> documents = new HashMap<>();

    public FileClassParser(Path root, Map<String, String> files) {
        this.root = root;
        compilerClasses.put(MachineProxyUsable.NAME, MachineProxyUsable.INSTANCE);
        this.files = new HashMap<>(files);
    }

    public void addClassParser(Parser parser) {
        parsers.put(parser.name(), parser);
    }

    public boolean parse(String name) {
        if (parsed.contains(name)) return true;
        if (!files.containsKey(name)) return false;

        String input;
        try {
            Path filePath = root.resolve(files.get(name));
            input = Files.readString(filePath);
        } catch (IOException e) {
            return false;
        }
        Pattern pattern = Pattern.compile("parser ([a-zA-Z]+);.*");
        Matcher matcher = pattern.matcher(input);
        boolean found = matcher.find();
        if (!found) return false;
        String parserName = matcher.group(1);

        if (!parsers.containsKey(parserName)) return false;
        Parser parser = parsers.get(parserName);
        parser.parse(this, input);

        // File has been parsed
        parsed.add(name);
        return true;
    }

    public Sources root() {
        return this;
    }

    public void add(Usable c) {
        compilerClasses.put(c.name(), c);
    }
    
    public void add(Compilable c) {
        compilableClasses.put(c.name(), c);
    }

    public Usable usable(String name) {
        return compilerClasses.get(name);
    }

    public Compilable compilable(String name) {
        return compilableClasses.get(name);
    }

    public Document document(String name) {
        return documents.get(name);
    }

    public void add(Document d) {
        documents.put(d.name, d);
    }
}
