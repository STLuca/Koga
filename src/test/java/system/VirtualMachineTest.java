package system;

import core.Document;
import language.core.Compiler;
import language.core.Compilable;
import language.compiling.DocumentBuilder;
import language.host.HostParser;
import language.hosted.HostedParser;
import language.interfaces.InterfaceParser;
import language.machine.MachineEnumParser;
import language.machine.MachineReferenceParser;
import language.machine.MachineUsableParser;
import language.parsing.FileClassParser;
import language.protocol.ProtocolParser;
import language.structure.StructureParser;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

public class VirtualMachineTest {

    FileClassParser parser;

    {
        HashMap<String, String> srcMap = new HashMap<>();

        // Machine classes
        srcMap.put("AdminRef", "machine/AdminRef");
        srcMap.put("Array", "machine/Array");
        srcMap.put("ArrayPointer", "machine/ArrayPointer");
        srcMap.put("Boolean", "machine/Boolean");
        srcMap.put("BumpAdminTask", "machine/BumpAdminTask");
        srcMap.put("Byte", "machine/Byte");
        srcMap.put("Char", "machine/Char");
        srcMap.put("Connection", "machine/Connection");
        srcMap.put("Debug", "machine/Debug");
        srcMap.put("Deferrer", "machine/Deferrer");
        srcMap.put("Exit", "machine/Exit");
        srcMap.put("If", "machine/If");
        srcMap.put("Int", "machine/Int");
        srcMap.put("InterfaceReference", "machine/InterfaceReference");
        srcMap.put("Logician", "machine/Logician");
        srcMap.put("Usable", "machine/Usable");
        srcMap.put("MemberReference", "machine/MemberReference");
        srcMap.put("Optional", "machine/Optional");
        srcMap.put("Pointer", "machine/Pointer");
        srcMap.put("Reference", "machine/Reference");
        srcMap.put("Return", "machine/Return");
        srcMap.put("Seq", "machine/Seq");
        srcMap.put("Status", "machine/Status");
        srcMap.put("String", "machine/String");
        srcMap.put("Switch", "machine/Switch");
        srcMap.put("InputStream", "machine/InputStream");
        srcMap.put("OutputStream", "machine/OutputStream");
        srcMap.put("SystemIn", "machine/SystemIn");
        srcMap.put("SystemOut", "machine/SystemOut");
        srcMap.put("Task", "machine/Task");
        srcMap.put("Throw", "machine/Throw");
        srcMap.put("Try", "machine/Try");
        srcMap.put("While", "machine/While");

        // structures
        srcMap.put("SimpleAdminTask", "structures/SimpleAdminTask");
        srcMap.put("LocalDate", "structures/LocalDate");

        // Reference classes
        srcMap.put("ArrayList", "reference/ArrayList");
        srcMap.put("Chat", "reference/Chat");
        srcMap.put("Fields", "reference/Fields");
        srcMap.put("SimpleAdministrator", "reference/SimpleAdministrator");

        // interface classes
        srcMap.put("Administrator", "interfaces/Administrator");
        srcMap.put("List", "interfaces/List");

        // protocols
        srcMap.put("Talker", "protocol/Talker");
        srcMap.put("Chatting", "protocol/Chatting");

        // System classes
        srcMap.put("AllocatorTest", "system/AllocatorTest");
        srcMap.put("ArrayPointerTest", "system/ArrayPointerTest");
        srcMap.put("ArrayListTest", "system/ArrayListTest");
        srcMap.put("ByteAndIntTest", "system/ByteAndIntTest");
        srcMap.put("ImplicitArgumentTest", "system/ImplicitArgumentTest");
        srcMap.put("CharTest", "system/CharTest");
        srcMap.put("ConstantsTest", "system/ConstantsTest");
        srcMap.put("DeferTest", "system/DeferTest");
        srcMap.put("EnumTest", "system/EnumTest");
        srcMap.put("ExceptionTest", "system/ExceptionTest");
        srcMap.put("IfTest", "system/IfTest");
        srcMap.put("IntCompareToBoolTest", "system/IntCompareToBoolTest");
        srcMap.put("ListTest", "system/ListTest");
        srcMap.put("LocalVariableTest", "system/LocalVariableTest");
        srcMap.put("MethodInvokeTest", "system/MethodInvokeTest");
        srcMap.put("MethodOverloadTest", "system/MethodOverloadTest");
        srcMap.put("MethodVariableTest", "system/MethodVariableTest");
        srcMap.put("NewTest", "system/NewTest");
        srcMap.put("NewThreadTest", "system/NewThreadTest");
        srcMap.put("PointerTest", "system/PointerTest");
        srcMap.put("ProxyTest", "system/ProxyTest");
        srcMap.put("Server", "system/Server");
        srcMap.put("StringEqualsTest", "system/StringEqualsTest");
        srcMap.put("StructureTest", "system/StructureTest");
        srcMap.put("SwitchTest", "system/SwitchTest");
        srcMap.put("TalkerTest", "system/TalkerTest");
        srcMap.put("TryTest", "system/TryTest");
        srcMap.put("UsingReferenceTest", "system/UsingReferenceTest");
        srcMap.put("WhileTest", "system/WhileTest");
        srcMap.put("WhileWithBreakTest", "system/WhileWithBreakTest");

        Path path;
        try {
            path = Path.of(getClass().getClassLoader().getResource("code/").toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        parser = new FileClassParser(path, srcMap);
        parser.addClassParser(new MachineUsableParser());
        parser.addClassParser(new MachineReferenceParser());
        parser.addClassParser(new MachineEnumParser());
        parser.addClassParser(new HostedParser());
        parser.addClassParser(new StructureParser());
        parser.addClassParser(new HostParser());
        parser.addClassParser(new InterfaceParser());
        parser.addClassParser(new ProtocolParser());
    }

    Administrator administrator(String... classNames) {
        Administrator administrator = Administrator.boot();

        // Parse all required dependencies
        ArrayList<Compilable> dependencies = new ArrayList<>();
        Set<String> haveCompiled = new HashSet<>();
        Deque<String> toCompile = new ArrayDeque<>(Arrays.asList(classNames));
        while (!toCompile.isEmpty()) {
            String compile = toCompile.pop();
            if (haveCompiled.contains(compile)) continue;
            parser.parse(compile);
            Compilable compiled = parser.compilable(compile);
            toCompile.addAll(compiled.dependencies());
            haveCompiled.add(compile);
            dependencies.addFirst(compiled);
        }

        // Compile all dependencies
        for (Compilable dependency : dependencies) {
            Compiler compiler = new DocumentBuilder();
            dependency.compile(parser, compiler);
            Document document = compiler.document();
            parser.add(document);
            administrator.integrate(document);
        }

        return administrator;
    }

    Inspector run(String docName) {
        Administrator administrator = administrator(docName);
        int instance = administrator.initHost(docName);
        Administrator.LogicianQuota t = administrator.find(instance).orElseThrow();

        while (t.status != Administrator.LogicianQuota.Status.Complete) {
            administrator.m.tick();
        }
        return administrator.inspect(instance);
    }

    @Test
    void byteAndIntWorks() {
        Inspector i = run("ByteAndIntTest");
        Inspector.Task t = i.tasks.get(0);
        int xVal = t.altData.get("x").get("val");
        int yVal = t.altData.get("y").get("val");
        int zVal = t.altData.get("z").get("val");
        assertThat(xVal).isEqualTo(10);
        assertThat(yVal).isEqualTo(25);
        assertThat(zVal).isEqualTo(111);
    }

    @Test
    void charWorks() {
        Inspector i = run("CharTest");
        Inspector.Task t = i.tasks.get(0);
        int xVal = t.altData.get("x").get("val");
        int yVal = t.altData.get("y").get("val");
        int zVal = t.altData.get("z").get("val");
        assertThat(xVal).isEqualTo(97);
        assertThat(yVal).isEqualTo(104);
        assertThat(zVal).isEqualTo(76);
    }

    @Test
    void ifWorks() {
        Inspector i = run("IfTest");
        Inspector.Task t = i.tasks.get(0);
        int xVal = t.altData.get("x").get("val");
        assertThat(xVal).isEqualTo(20);
    }

    @Test
    void whileWorks() {
        Inspector i = run("WhileTest");
        Inspector.Task t = i.tasks.get(0);
        int xVal = t.altData.get("x").get("val");
        int yVal = t.altData.get("y").get("val");
        assertThat(xVal).isEqualTo(10);
        assertThat(yVal).isEqualTo(0);
    }

    @Test
    void whileWithBreakWorks() {
        Inspector i = run("WhileWithBreakTest");
        Inspector.Task t = i.tasks.get(0);
        int xVal = t.altData.get("x").get("val");
        int yVal = t.altData.get("y").get("val");
        assertThat(xVal).isEqualTo(1);
        assertThat(yVal).isEqualTo(1);
    }

    @Test
    void basicSwitchWorks() {
        Inspector i = run("SwitchTest");
        Inspector.Task t = i.tasks.get(0);
        int classSymbol = t.altData.get("x").get("val");
        assertThat(classSymbol).isEqualTo(10);
    }

    @Test
    void pointerWorks() {
        Inspector i = run("PointerTest");
        Inspector.Task t = i.tasks.get(0);
        int xVal = t.altData.get("x").get("val");
        int yVal = t.altData.get("y").get("val");
        int zVal = t.altData.get("z").get("val");
        assertThat(xVal).isEqualTo(3);
        assertThat(yVal).isEqualTo(1);
        assertThat(zVal).isEqualTo(3);
    }

    @Test
    void intCompareIntoBoolWorks() {
        // unsigned memes def don't work
        Inspector i = run("IntCompareToBoolTest");
        Inspector.Task t = i.tasks.get(0);
        int yVal = t.altData.get("y").get("val");
        assertThat(yVal).isEqualTo(1);
    }

    @Test
    void arrayPointerWorks() {
        Inspector i = run("ArrayPointerTest");
        Inspector.Task t = i.tasks.get(0);
        int xVal = t.altData.get("x").get("val");
        int yVal = t.altData.get("y").get("val");
        int zVal = t.altData.get("z").get("val");
        assertThat(xVal).isEqualTo(4);
        assertThat(yVal).isEqualTo(5);
        assertThat(zVal).isEqualTo(5);
    }

    @Test
    void proxyWorks() {
        Inspector i = run("ProxyTest");
        Inspector.Task t = i.tasks.get(0);
        int yVal = t.altData.get("y").get("val");
        assertThat(yVal).isEqualTo(3);
    }

    @Test
    void machineMethodVariableWorks() {
        Inspector i = run("MethodVariableTest");
        Inspector.Task t = i.tasks.get(0);
        int xVal = t.altData.get("x").get("val");
        assertThat(xVal).isEqualTo(14);
    }

    @Test
    void methodInvokeWorks() {
        Inspector i = run("MethodInvokeTest");
        Inspector.Task t = i.tasks.get(0);
        int xVal = t.altData.get("x").get("val");
        int yVal = t.altData.get("y").get("val");
        assertThat(xVal).isEqualTo(5);
        assertThat(yVal).isEqualTo(2);
    }

    @Test
    void localVariableWorks() {
        Inspector i = run("LocalVariableTest");
        Inspector.Task t = i.tasks.get(0);
        int yVal = t.altData.get("y").get("val");
        assertThat(yVal).isEqualTo(16);
    }

    @Test
    void usingReferenceInstanceWork() {
        Inspector i = run("UsingReferenceTest");
        byte[] bytes = new byte[4];
        i.load(16392, bytes);
        int fieldX = bytes[0];
        assertThat(fieldX).isEqualTo(99);
    }

    @Test
    void arrayListTest() {
        Inspector i = run("ArrayListTest");
        byte[] bytes = new byte[20];
        i.load(16680, bytes);
        int fieldX = bytes[12];
        assertThat(fieldX).isEqualTo(99);
    }

    @Test
    void listTest() {
        Inspector i = run("ListTest");
        byte[] bytes = new byte[20];
        i.load(16680, bytes);
        int fieldX = bytes[12];
        assertThat(fieldX).isEqualTo(99);
    }

    @Test
    void exceptionTest() {
        Inspector i = run("ExceptionTest");
        Inspector.Task t = i.tasks.get(0);
        int yVal = t.altData.get("x").get("val");
        assertThat(yVal).isEqualTo(10);
    }

    @Test
    void constantsTest() {
        Inspector i = run("ConstantsTest");
        Inspector.Task t = i.tasks.get(0);
        int xVal = t.altData.get("x").get("val");
        int yVal = t.altData.get("y").get("val");
        assertThat(xVal).isEqualTo(67);
        assertThat(yVal).isEqualTo(111);
    }

    @Test
    void clientAndServerWithConnection() {
        String clientName = "TalkerTest";
        String serverName = "Server";
        Administrator administrator = administrator(clientName, serverName);
        int clientInstance = administrator.initHost(clientName);
        int serverInstance = administrator.initHost(serverName);

        Administrator.LogicianQuota clientThread = administrator.find(clientInstance).orElseThrow();
        administrator.schedule(clientThread);
        while (clientThread.status != Administrator.LogicianQuota.Status.Complete) {
            administrator.m.tick();
        }

        Inspector i = administrator.inspect(0);
        Inspector.Task t = i.tasks.get(0);
        int stringStart = t.altData.get("result").get("start");
        int stringSize = t.altData.get("result").get("size");
        assertThat(stringSize).isEqualTo(12);
        byte[] bytes = new byte[stringSize];
        i.load(stringStart, bytes);
        assertThat(bytes).isEqualTo("hello client".getBytes());
    }

    @Test
    void allocatorWorks() {
        Inspector i = run("AllocatorTest");
        Inspector.Task t = i.tasks.get(0);
        int allocateOneVal = t.altData.get("allocateOne").get("val");
        int allocateTwoVal = t.altData.get("allocateTwo").get("val");
        assertThat(allocateOneVal).isEqualTo(16392);
        assertThat(allocateTwoVal).isEqualTo(16516);
    }

    @Test
    void enumWorks() {
        Inspector i = run("EnumTest");
        Inspector.Task t = i.tasks.get(0);
        int xVal = t.altData.get("x").get("val");
        assertThat(xVal).isEqualTo(4);
    }

    @Test
    void methodOverloadWorks() {
        Inspector i = run("MethodOverloadTest");
        Inspector.Task t = i.tasks.get(0);
        int xVal = t.altData.get("x").get("val");
        assertThat(xVal).isEqualTo(10);
    }

    @Test
    void callerArgumentWorks() {
        Inspector i = run("ImplicitArgumentTest");
        Inspector.Task t = i.tasks.get(0);
        int bVal = t.altData.get("b").get("val");
        int cVal = t.altData.get("c").get("val");
        assertThat(bVal).isEqualTo(0);
        assertThat(cVal).isEqualTo(1);
    }

    @Test
    void stringEqualsWorks() {
        Inspector i = run("StringEqualsTest");
        Inspector.Task t = i.tasks.get(0);
        int bVal = t.altData.get("b").get("val");
        assertThat(bVal).isEqualTo(1);
    }

    @Test
    void tmp() {
        Inspector i = run("NewTest");
        Inspector.Task t = i.tasks.get(0);
        int bVal = t.altData.get("x").get("val");
        assertThat(bVal).isEqualTo(14);
    }

    @Test
    void tryWorks() {
        Inspector i = run("TryTest");
        Inspector.Task t = i.tasks.get(0);
        int bVal = t.altData.get("x").get("val");
        assertThat(bVal).isEqualTo(30);
    }

    @Test
    void structureWorks() {
        Inspector i = run("StructureTest");
        Inspector.Task t = i.tasks.get(0);
        int todayYear  = t.altData.get("today").get("year");
        int todayMonth = t.altData.get("today").get("month");
        int todayDay   = t.altData.get("today").get("day");
        int dateYear   = t.altData.get("date").get("year");
        int dateMonth  = t.altData.get("date").get("month");
        int dateDay    = t.altData.get("date").get("day");
        assertThat(todayYear).isEqualTo(2025);
        assertThat(todayMonth).isEqualTo(3);
        assertThat(todayDay).isEqualTo(6);
        assertThat(dateYear).isEqualTo(0);
        assertThat(dateMonth).isEqualTo(1);
        assertThat(dateDay).isEqualTo(4);
    }

    @Test
    void deferWorks() {
        Inspector i = run("DeferTest");
        Inspector.Task t = i.tasks.get(0);
        int xVal = t.altData.get("x").get("val");
        assertThat(xVal).isEqualTo(10);
    }

}
