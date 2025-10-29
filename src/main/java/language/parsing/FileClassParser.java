package language.parsing;

import core.Document;
import language.compiling.DocumentBuilder;
import language.core.Compilable;
import language.core.Parser;
import language.core.Sources;
import language.core.Structure;
import language.machine.MachineProxyStructure;

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
    HashMap<String, Structure> compilerClasses = new HashMap<>();
    HashMap<String, Document> documents = new HashMap<>();
    HashMap<String, Document> headDocuments = new HashMap<>();

    public FileClassParser(Path root, Map<String, String> files) {
        this.root = root;
        compilerClasses.put(MachineProxyStructure.NAME, MachineProxyStructure.INSTANCE);
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

    public void add(Structure c) {
        compilerClasses.put(c.name(), c);
    }
    
    public void add(Compilable c) {
        compilableClasses.put(c.name(), c);
    }

    public Structure structure(String name) {
        return compilerClasses.get(name);
    }

    public Document document(String name, Compilable.Level level) {
        switch (level) {
            case Head -> {
                if (headDocuments.containsKey(name)) {
                    return headDocuments.get(name);
                }
                if (compilableClasses.containsKey(name)) {
                    DocumentBuilder compiler = new DocumentBuilder();
                    compilableClasses.get(name).compile(this, compiler, level);
                    Document document = compiler.document();
                    headDocuments.put(name, document);
                    return document;
                }
            }
            case Full -> {
                if (documents.containsKey(name)) {
                    return documents.get(name);
                }
                if (compilableClasses.containsKey(name)) {
                    DocumentBuilder compiler = new DocumentBuilder();
                    compilableClasses.get(name).compile(this, compiler, level);
                    Document document = compiler.document();
                    documents.put(name, document);
                    return document;
                }
                return documents.get(name);
            }
            default -> {
                throw new RuntimeException();
            }
        }
        throw new RuntimeException();
    }
}
