package language.parsing;

import language.core.Classes;
import language.core.Parser;
import language.core.Compilable;
import language.core.Usable;
import language.interfaces.InterfaceParser;
import language.machine.MachineUsableParser;
import language.machine.MachineEnumParser;
import language.machine.MachineProxyUsable;
import language.machine.MachineReferenceParser;
import language.protocol.ProtocolParser;
import language.reference.ReferenceParser;
import language.system.SystemParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class FileClassParser implements Classes {

    Map<String, String> files = new HashMap<>();

    Map<String, Parser> parsers = new LinkedHashMap<>();

    Map<String, Compilable> compilableClasses = new HashMap<>();
    Map<String, Usable> compilerClasses = new HashMap<>();

    public FileClassParser(Map<String, String> files) {
        addClassParser(new MachineUsableParser());
        addClassParser(new MachineReferenceParser());
        addClassParser(new MachineEnumParser());
        addClassParser(new ProtocolParser());
        addClassParser(new ReferenceParser());
        addClassParser(new SystemParser());
        addClassParser(new InterfaceParser());

        compilerClasses.put(MachineProxyUsable.NAME, MachineProxyUsable.INSTANCE);
        this.files = files;
    }

    public void addClassParser(Parser parser) {
        parsers.put(parser.name(), parser);
    }

    public boolean parse(String name) {
        String parserName = readParserName(name);
        if (parserName == null) return false;
        if (!parsers.containsKey(parserName)) throw new RuntimeException("No parser for " + parserName);
        Parser parser = parsers.get(parserName);
        if (parser == null) return false;
        parser.parse(this, readNew(name));
        return true;
    }

    private String readNew(String name) {
        if (!files.containsKey(name)) throw new RuntimeException("class doesn't exist");
        InputStream is = getClass().getClassLoader().getResourceAsStream("code/" + files.get(name));
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        return br.lines().skip(1).collect(Collectors.joining(System.lineSeparator())).replace("\r", "");
    }

    private String readParserName(String name) {
        try {
            if (!files.containsKey(name)) return null; // throw new RuntimeException("class doesn't exist");
            InputStream is = getClass().getClassLoader().getResourceAsStream("code/" + files.get(name));
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            Pattern pattern = Pattern.compile("parser ([a-zA-Z]+);.*");
            String line = br.readLine();
            Matcher matcher = pattern.matcher(line);
            matcher.find();
            return matcher.group(1);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    
    public void add(Usable c) {
        compilerClasses.put(c.name(), c);
    }

    
    public void add(Compilable c) {
        compilableClasses.put(c.name(), c);
    }

    public Compilable compilable(String name) {
        if (!compilableClasses.containsKey(name)) {
            parse(name);
        }
        return compilableClasses.get(name);
    }

    
    public Usable usable(String name) {
        if (!compilerClasses.containsKey(name)) {
            boolean res = parse(name);
            if (!res) return null;
        }
        return compilerClasses.get(name);
    }
}
