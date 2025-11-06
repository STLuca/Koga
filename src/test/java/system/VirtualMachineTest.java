package system;

import core.Document;
import language.core.Compilable;
import language.host.HostParser;
import language.hosted.HostedParser;
import language.interfaces.InterfaceParser;
import language.machine.MachineEnumParser;
import language.machine.MachineReferenceParser;
import language.machine.MachineCompositeParser;
import language.parsing.FileClassParser;
import language.protocol.ProtocolParser;
import language.composite.CompositeParser;
import language.enumeration.EnumParser;
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
        srcMap.put("core.AdminRef", "machine/AdminRef");
        srcMap.put("core.Array", "machine/Array");
        srcMap.put("core.ArrayAccessor", "machine/ArrayAccessor");
        srcMap.put("core.Boolean", "machine/Boolean");
        srcMap.put("core.BumpAdminTask", "machine/BumpAdminTask");
        srcMap.put("core.Byte", "machine/Byte");
        srcMap.put("core.Char", "machine/Char");
        srcMap.put("core.Connection", "machine/Connection");
        srcMap.put("core.Debug", "machine/Debug");
        srcMap.put("core.Deferrer", "machine/Deferrer");
        srcMap.put("core.Exit", "machine/Exit");
        srcMap.put("core.If", "machine/If");
        srcMap.put("core.Int", "machine/Int");
        srcMap.put("core.InterfaceReference", "machine/InterfaceReference");
        srcMap.put("core.Logician", "machine/Logician");
        srcMap.put("core.MemberReference", "machine/MemberReference");
        srcMap.put("core.Optional", "machine/Optional");
        srcMap.put("core.Pointer", "machine/Pointer");
        srcMap.put("core.Reference", "machine/Reference");
        srcMap.put("core.Return", "machine/Return");
        srcMap.put("core.Seq", "machine/Seq");
        srcMap.put("core.Status", "machine/Status");
        srcMap.put("core.String", "machine/String");
        srcMap.put("core.Structure", "machine/Structure");
        srcMap.put("core.Switch", "machine/Switch");
        srcMap.put("core.InputStream", "machine/InputStream");
        srcMap.put("core.OutputStream", "machine/OutputStream");
        srcMap.put("core.SystemIn", "machine/SystemIn");
        srcMap.put("core.SystemOut", "machine/SystemOut");
        srcMap.put("core.Task", "machine/Task");
        srcMap.put("core.Throw", "machine/Throw");
        srcMap.put("core.Try", "machine/Try");
        srcMap.put("core.While", "machine/While");

        // composites
        srcMap.put("core.SimpleAdminTask", "composites/SimpleAdminTask");
        srcMap.put("core.LocalDate", "composites/LocalDate");

        // enums
        srcMap.put("parser.ParserContext", "enums/ParserContext");

        // Reference classes
        srcMap.put("collection.ArrayList", "reference/ArrayList");
        srcMap.put("chatting.Chat", "reference/Chat");
        srcMap.put("test.Fields", "reference/Fields");
        srcMap.put("util.SimpleAdministrator", "reference/SimpleAdministrator");

        // interface classes
        srcMap.put("core.Administrator", "interfaces/Administrator");
        srcMap.put("collection.SimpleList", "interfaces/List");

        // protocols
        srcMap.put("chatting.Talker", "protocol/Talker");
        srcMap.put("chatting.Chatting", "protocol/Chatting");

        // collections
        srcMap.put("collection.List", "collections/List");
        srcMap.put("collection.ListAccessor", "collections/ListAccessor");
        srcMap.put("collection.ListIterator", "collections/ListIterator");
        srcMap.put("collection.StaticList", "collections/StaticList");
        srcMap.put("collection.StaticListAccessor", "collections/StaticListAccessor");


        // System classes
        srcMap.put("test.AllocatorTest", "system/AllocatorTest");
        srcMap.put("test.ArrayListTest", "system/ArrayListTest");
        srcMap.put("test.ArrayAccessorTest", "system/ArrayAccessorTest");
        srcMap.put("test.ByteAndIntTest", "system/ByteAndIntTest");
        srcMap.put("test.ImplicitArgumentTest", "system/ImplicitArgumentTest");
        srcMap.put("test.CharTest", "system/CharTest");
        srcMap.put("test.ConstantsTest", "system/ConstantsTest");
        srcMap.put("test.DeferTest", "system/DeferTest");
        srcMap.put("test.EnumTest", "system/EnumTest");
        srcMap.put("test.ExceptionTest", "system/ExceptionTest");
        srcMap.put("test.IfTest", "system/IfTest");
        srcMap.put("test.IntCompareToBoolTest", "system/IntCompareToBoolTest");
        srcMap.put("test.ListTest", "system/ListTest");
        srcMap.put("test.LocalVariableTest", "system/LocalVariableTest");
        srcMap.put("test.MethodInvokeTest", "system/MethodInvokeTest");
        srcMap.put("test.MethodOverloadTest", "system/MethodOverloadTest");
        srcMap.put("test.MethodVariableTest", "system/MethodVariableTest");
        srcMap.put("test.NewTest", "system/NewTest");
        srcMap.put("test.NewThreadTest", "system/NewThreadTest");
        srcMap.put("test.PointerTest", "system/PointerTest");
        srcMap.put("test.ProxyTest", "system/ProxyTest");
        srcMap.put("test.Server", "system/Server");
        srcMap.put("test.StaticListTest", "system/StaticListTest");
        srcMap.put("test.StringEqualsTest", "system/StringEqualsTest");
        srcMap.put("test.StructureTest", "system/StructureTest");
        srcMap.put("test.SwitchTest", "system/SwitchTest");
        srcMap.put("test.TalkerTest", "system/TalkerTest");
        srcMap.put("test.TryTest", "system/TryTest");
        srcMap.put("test.UnionTest", "system/UnionTest");
        srcMap.put("test.UsingReferenceTest", "system/UsingReferenceTest");
        srcMap.put("test.WhileTest", "system/WhileTest");
        srcMap.put("test.WhileWithBreakTest", "system/WhileWithBreakTest");

        Path path;
        try {
            path = Path.of(getClass().getClassLoader().getResource("code/").toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        parser = new FileClassParser(path, srcMap);
        parser.addClassParser(new MachineCompositeParser());
        parser.addClassParser(new MachineReferenceParser());
        parser.addClassParser(new MachineEnumParser());
        parser.addClassParser(new HostedParser());
        parser.addClassParser(new CompositeParser());
        parser.addClassParser(new EnumParser());
        parser.addClassParser(new HostParser());
        parser.addClassParser(new InterfaceParser());
        parser.addClassParser(new ProtocolParser());
    }

    Administrator administrator(String... classNames) {
        Administrator administrator = Administrator.boot();

        // Parse all required dependencies
        ArrayList<Document> dependencies = new ArrayList<>();
        Set<String> haveCompiled = new HashSet<>();
        Deque<String> toCompile = new ArrayDeque<>(Arrays.asList(classNames));
        while (!toCompile.isEmpty()) {
            String compile = toCompile.pop();
            if (haveCompiled.contains(compile)) continue;
            parser.parse(compile);
            Document compiled = parser.document(compile, Compilable.Level.Head);
            if (compiled.dependencies != null) {
                toCompile.addAll(Arrays.asList(compiled.dependencies));
            }
            haveCompiled.add(compile);
            dependencies.addFirst(compiled);
        }

        // Compile all dependencies
        for (Document dependency : dependencies) {
            Document document = parser.document(dependency.name, Compilable.Level.Full);
            byte[] bytes = document.bytes();
            administrator.integrate(bytes);
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
        Inspector i = run("test.ByteAndIntTest");
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
        Inspector i = run("test.CharTest");
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
        Inspector i = run("test.IfTest");
        Inspector.Task t = i.tasks.get(0);
        int xVal = t.altData.get("x").get("val");
        assertThat(xVal).isEqualTo(20);
    }

    @Test
    void whileWorks() {
        Inspector i = run("test.WhileTest");
        Inspector.Task t = i.tasks.get(0);
        int xVal = t.altData.get("x").get("val");
        int yVal = t.altData.get("y").get("val");
        assertThat(xVal).isEqualTo(10);
        assertThat(yVal).isEqualTo(0);
    }

    @Test
    void whileWithBreakWorks() {
        Inspector i = run("test.WhileWithBreakTest");
        Inspector.Task t = i.tasks.get(0);
        int xVal = t.altData.get("x").get("val");
        int yVal = t.altData.get("y").get("val");
        assertThat(xVal).isEqualTo(1);
        assertThat(yVal).isEqualTo(1);
    }

    @Test
    void basicSwitchWorks() {
        Inspector i = run("test.SwitchTest");
        Inspector.Task t = i.tasks.get(0);
        int classSymbol = t.altData.get("x").get("val");
        assertThat(classSymbol).isEqualTo(10);
    }

    @Test
    void pointerWorks() {
        Inspector i = run("test.PointerTest");
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
        Inspector i = run("test.IntCompareToBoolTest");
        Inspector.Task t = i.tasks.get(0);
        int yVal = t.altData.get("y").get("val");
        assertThat(yVal).isEqualTo(1);
    }

    @Test
    void ArrayAccessorWorks() {
        Inspector i = run("test.ArrayAccessorTest");
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
        Inspector i = run("test.ProxyTest");
        Inspector.Task t = i.tasks.get(0);
        int yVal = t.altData.get("y").get("val");
        assertThat(yVal).isEqualTo(3);
    }

    @Test
    void machineMethodVariableWorks() {
        Inspector i = run("test.MethodVariableTest");
        Inspector.Task t = i.tasks.get(0);
        int xVal = t.altData.get("x").get("val");
        assertThat(xVal).isEqualTo(14);
    }

    @Test
    void methodInvokeWorks() {
        Inspector i = run("test.MethodInvokeTest");
        Inspector.Task t = i.tasks.get(0);
        int xVal = t.altData.get("x").get("val");
        int yVal = t.altData.get("y").get("val");
        assertThat(xVal).isEqualTo(5);
        assertThat(yVal).isEqualTo(2);
    }

    @Test
    void localVariableWorks() {
        Inspector i = run("test.LocalVariableTest");
        Inspector.Task t = i.tasks.get(0);
        int yVal = t.altData.get("y").get("val");
        assertThat(yVal).isEqualTo(16);
    }

    @Test
    void usingReferenceInstanceWork() {
        Inspector i = run("test.UsingReferenceTest");
        byte[] bytes = new byte[4];
        i.load(16392, bytes);
        int fieldX = bytes[0];
        assertThat(fieldX).isEqualTo(99);
    }

    @Test
    void arrayListTest() {
        Inspector i = run("test.ArrayListTest");
        byte[] bytes = new byte[20];
        i.load(16680, bytes);
        int fieldX = bytes[12];
        assertThat(fieldX).isEqualTo(99);
    }

    @Test
    void listTest() {
        Inspector i = run("test.ListTest");
        byte[] bytes = new byte[20];
        i.load(16680, bytes);
        int fieldX = bytes[12];
        assertThat(fieldX).isEqualTo(99);
    }

    @Test
    void exceptionTest() {
        Inspector i = run("test.ExceptionTest");
        Inspector.Task t = i.tasks.get(0);
        int yVal = t.altData.get("x").get("val");
        assertThat(yVal).isEqualTo(10);
    }

    @Test
    void constantsTest() {
        Inspector i = run("test.ConstantsTest");
        Inspector.Task t = i.tasks.get(0);
        int xVal = t.altData.get("x").get("val");
        int yVal = t.altData.get("y").get("val");
        assertThat(xVal).isEqualTo(67);
        assertThat(yVal).isEqualTo(111);
    }

    @Test
    void clientAndServerWithConnection() {
        String clientName = "test.TalkerTest";
        String serverName = "test.Server";
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
        Inspector i = run("test.AllocatorTest");
        Inspector.Task t = i.tasks.get(0);
        int allocateOneVal = t.altData.get("allocateOne").get("val");
        int allocateTwoVal = t.altData.get("allocateTwo").get("val");
        assertThat(allocateOneVal).isEqualTo(16392);
        assertThat(allocateTwoVal).isEqualTo(16516);
    }

    @Test
    void enumWorks() {
        Inspector i = run("test.EnumTest");
        Inspector.Task t = i.tasks.get(0);
        int xVal = t.altData.get("x").get("val");
        assertThat(xVal).isEqualTo(4);
    }

    @Test
    void methodOverloadWorks() {
        Inspector i = run("test.MethodOverloadTest");
        Inspector.Task t = i.tasks.get(0);
        int xVal = t.altData.get("x").get("val");
        assertThat(xVal).isEqualTo(10);
    }

    @Test
    void callerArgumentWorks() {
        Inspector i = run("test.ImplicitArgumentTest");
        Inspector.Task t = i.tasks.get(0);
        int bVal = t.altData.get("b").get("val");
        int cVal = t.altData.get("c").get("val");
        assertThat(bVal).isEqualTo(0);
        assertThat(cVal).isEqualTo(1);
    }

    @Test
    void stringEqualsWorks() {
        Inspector i = run("test.StringEqualsTest");
        Inspector.Task t = i.tasks.get(0);
        int bVal = t.altData.get("b").get("val");
        assertThat(bVal).isEqualTo(1);
    }

    @Test
    void tmp() {
        Inspector i = run("test.NewTest");
        Inspector.Task t = i.tasks.get(0);
        int bVal = t.altData.get("x").get("val");
        assertThat(bVal).isEqualTo(14);
    }

    @Test
    void tryWorks() {
        Inspector i = run("test.TryTest");
        Inspector.Task t = i.tasks.get(0);
        int bVal = t.altData.get("x").get("val");
        assertThat(bVal).isEqualTo(30);
    }

    @Test
    void structureWorks() {
        Inspector i = run("test.StructureTest");
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
        Inspector i = run("test.DeferTest");
        Inspector.Task t = i.tasks.get(0);
        int xVal = t.altData.get("x").get("val");
        assertThat(xVal).isEqualTo(10);
    }

    @Test
    void unionWorks() {
        Inspector i = run("test.UnionTest");
        Inspector.Task t = i.tasks.get(0);
        int xVal = t.altData.get("x").get("val");
        assertThat(xVal).isEqualTo(8);
    }

    @Test
    void staticListTest() {
        Inspector i = run("test.StaticListTest");
        String is = i.toString();
        Inspector.Task t = i.tasks.get(0);
        int xVal = t.altData.get("writeElement").get("val");
        assertThat(xVal).isEqualTo(19);
        int sumVal = t.altData.get("sum").get("val");
        assertThat(sumVal).isEqualTo(40);
    }

}
