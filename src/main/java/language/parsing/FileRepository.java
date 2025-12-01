package language.parsing;

import language.core.*;
import language.machine.MachineProxyStructure;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileRepository implements Repository {

    Path root;
    HashMap<String, String> files;
    HashSet<String> parsed = new HashSet<>();

    LinkedHashMap<String, Parser> parsers = new LinkedHashMap<>();

    HashMap<String, Compilable> compilableClasses = new HashMap<>();
    HashMap<String, Structure> compilerClasses = new HashMap<>();

    HashMap<String, Document> cachedDocuments = new HashMap<>();

    public FileRepository(Path root, Map<String, String> files) {
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
        Parser.Output output = parser.parse(input);
        int nameIndex = 0;
        if (output.structures != null) {
            for (Structure structure : output.structures) {
                compilerClasses.put(output.names[nameIndex++], structure);
            }
        }
        if (output.compilables != null) {
            for (Compilable compilable : output.compilables) {
                compilableClasses.put(output.names[nameIndex++], compilable);
            }
        }

        // File has been parsed
        parsed.add(name);
        return true;
    }

    public Optional<Structure> structure(String name) {
        parse(name);
        return Optional.ofNullable(compilerClasses.get(name));
    }

    public Optional<Compilable> compilable(String name) {
        parse(name);
        return Optional.ofNullable(compilableClasses.get(name));
    }

    public Optional<language.core.Document> document(String name) {
        parse(name);
        if (cachedDocuments.containsKey(name)) {
            return Optional.of(cachedDocuments.get(name));
        }
        Compilable compilable = compilableClasses.get(name);
        Document document = compilable.document();
        cachedDocuments.put(name, document);
        return Optional.ofNullable(document);
    }
}
