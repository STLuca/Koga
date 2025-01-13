package system;

import language.core.Compiler;
import language.core.Compilable;
import core.Class;
import language.compiling.ClassBuilder;
import language.parsing.FileClassParser;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

public class VirtualMachineTest {

    FileClassParser parser;

    {
        Map<String, String> srcMap = new HashMap<>();

        // Machine classes
        srcMap.put("AdminRef", "machine/AdminRef");
        srcMap.put("Array", "machine/Array");
        srcMap.put("ArrayPointer", "machine/ArrayPointer");
        srcMap.put("Boolean", "machine/Boolean");
        srcMap.put("BumpAdminTask", "machine/BumpAdminTask");
        srcMap.put("Byte", "machine/Byte");
        srcMap.put("Char", "machine/Char");
        srcMap.put("Debug", "machine/Debug");
        srcMap.put("Exit", "machine/Exit");
        srcMap.put("If", "machine/If");
        srcMap.put("Int", "machine/Int");
        srcMap.put("InterfaceReference", "machine/InterfaceReference");
        srcMap.put("Logician", "machine/Logician");
        srcMap.put("MachineClass", "machine/MachineClass");
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

        // Reference classes
        srcMap.put("ArrayList", "reference/ArrayList");
        srcMap.put("BumpAdministrator", "reference/BumpAdministrator");
        srcMap.put("Fields", "reference/Fields");

        // interface classes
        srcMap.put("Administrator", "interfaces/Administrator");
        srcMap.put("List", "interfaces/List");

        // protocols
        srcMap.put("Talker", "protocol/Talker");

        // System classes
        srcMap.put("AllocatorTest", "system/AllocatorTest");
        srcMap.put("ArrayPointerTest", "system/ArrayPointerTest");
        srcMap.put("ArrayListTest", "system/ArrayListTest");
        srcMap.put("ByteAndIntTest", "system/ByteAndIntTest");
        srcMap.put("ImplicitArgumentTest", "system/ImplicitArgumentTest");
        srcMap.put("CharTest", "system/CharTest");
        srcMap.put("ConstantsTest", "system/ConstantsTest");
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
        srcMap.put("SwitchTest", "system/SwitchTest");
        srcMap.put("TalkerOne", "system/TalkerOneTest");
        srcMap.put("TalkerTwo", "system/TalkerTwoTest");
        srcMap.put("TryTest", "system/TryTest");
        srcMap.put("UsingReferenceTest", "system/UsingReferenceTest");
        srcMap.put("WhileTest", "system/WhileTest");
        srcMap.put("WhileWithBreakTest", "system/WhileWithBreakTest");

        parser = new FileClassParser(srcMap);
    }

    Administrator administrator(String... classNames) {
        Administrator administrator = Administrator.boot();
        for (String className : classNames) {
            Compilable c = parser.compilable(className);
            // TODO: copy from VirtualMachine?
            List<String> dependencies = new ArrayList<>(c.dependencies());
            Set<Compilable> dependenciesAdded = new HashSet<>();
            while (!dependencies.isEmpty()) {
                String dependency = dependencies.get(0);
                Compilable dc = parser.compilable(dependency);
                if (!dc.dependencies().isEmpty() && !dependenciesAdded.contains(dc)) {
                    dependencies.addAll(0, dc.dependencies());
                    dependenciesAdded.add(dc);
                    continue;
                }
                Compiler compiler = new ClassBuilder();
                dc.compile(parser, compiler);
                administrator.integrate(compiler.clazz());
                dependencies.remove(0);
            }
        /*
        for (String dependency : c.dependencies()) {
            CompilableClass dc = parser.compilableClass(dependency);
            Compiler compiler = new ClassBuilder();
            dc.compile(parser, compiler);
            vm.addClass(compiler.clazz());
        }
         */

            // compile the main class
            Compiler compiler = new ClassBuilder();
            c.compile(parser, compiler);
            administrator.integrate(compiler.clazz());
        }

        return administrator;
    }

    Inspector run(String clazzName) {
        Administrator administrator = administrator(clazzName);
        Class c = administrator.classes.get(clazzName);
        int instance = administrator.initClass(c);
        Administrator.LogicianQuota t = administrator.find(instance).orElseThrow();

        while (t.status != Administrator.Status.Complete) {
            administrator.m.tick();
        }
        return administrator.snapshot(instance);
    }

    @Test
    void byteAndIntWorks() {
        Inspector d = run("ByteAndIntTest");
        Inspector.Task f = d.tasks.get(0);
        int xVal = f.altData.get("x").get("val");
        int yVal = f.altData.get("y").get("val");
        int zVal = f.altData.get("z").get("val");
        assertThat(xVal).isEqualTo(10);
        assertThat(yVal).isEqualTo(25);
        assertThat(zVal).isEqualTo(111);
    }

    @Test
    void charWorks() {
        Inspector d = run("CharTest");
        Inspector.Task f = d.tasks.get(0);
        int xVal = f.altData.get("x").get("val");
        int yVal = f.altData.get("y").get("val");
        int zVal = f.altData.get("z").get("val");
        assertThat(xVal).isEqualTo(97);
        assertThat(yVal).isEqualTo(104);
        assertThat(zVal).isEqualTo(76);
    }

    @Test
    void ifWorks() {
        Inspector d = run("IfTest");
        Inspector.Task f = d.tasks.get(0);
        int xVal = f.altData.get("x").get("val");
        assertThat(xVal).isEqualTo(20);
    }

    @Test
    void whileWorks() {
        Inspector d = run("WhileTest");
        Inspector.Task f = d.tasks.get(0);
        int xVal = f.altData.get("x").get("val");
        int yVal = f.altData.get("y").get("val");
        assertThat(xVal).isEqualTo(10);
        assertThat(yVal).isEqualTo(0);
    }

    @Test
    void whileWithBreakWorks() {
        Inspector d = run("WhileWithBreakTest");
        Inspector.Task f = d.tasks.get(0);
        int xVal = f.altData.get("x").get("val");
        int yVal = f.altData.get("y").get("val");
        assertThat(xVal).isEqualTo(1);
        assertThat(yVal).isEqualTo(1);
    }

    @Test
    void basicSwitchWorks() {
        Inspector d = run("SwitchTest");
        Inspector.Task f = d.tasks.get(0);
        int classSymbol = f.altData.get("x").get("val");
        assertThat(classSymbol).isEqualTo(10);
    }

    @Test
    void pointerWorks() {
        Inspector d = run("PointerTest");
        Inspector.Task f = d.tasks.get(0);
        int xVal = f.altData.get("x").get("val");
        int yVal = f.altData.get("y").get("val");
        int zVal = f.altData.get("z").get("val");
        assertThat(xVal).isEqualTo(3);
        assertThat(yVal).isEqualTo(1);
        assertThat(zVal).isEqualTo(3);
    }

    @Test
    void intCompareIntoBoolWorks() {
        // unsigned memes def don't work
        Inspector d = run("IntCompareToBoolTest");
        Inspector.Task f = d.tasks.get(0);
        int yVal = f.altData.get("y").get("val");
        assertThat(yVal).isEqualTo(1);
    }

    @Test
    void arrayPointerWorks() {
        Inspector d = run("ArrayPointerTest");
        Inspector.Task f = d.tasks.get(0);
        int xVal = f.altData.get("x").get("val");
        int yVal = f.altData.get("y").get("val");
        int zVal = f.altData.get("z").get("val");
        assertThat(xVal).isEqualTo(4);
        assertThat(yVal).isEqualTo(5);
        assertThat(zVal).isEqualTo(5);
    }

    @Test
    void proxyWorks() {
        Inspector d = run("ProxyTest");
        Inspector.Task f = d.tasks.get(0);
        int yVal = f.altData.get("y").get("val");
        assertThat(yVal).isEqualTo(3);
    }

    @Test
    void machineMethodVariableWorks() {
        Inspector d = run("MethodVariableTest");
        Inspector.Task f = d.tasks.get(0);
        int xVal = f.altData.get("x").get("val");
        assertThat(xVal).isEqualTo(14);
    }

    @Test
    void methodInvokeWorks() {
        Inspector d = run("MethodInvokeTest");
        Inspector.Task f = d.tasks.get(0);
        int xVal = f.altData.get("x").get("val");
        int yVal = f.altData.get("y").get("val");
        assertThat(xVal).isEqualTo(5);
        assertThat(yVal).isEqualTo(2);
    }

    @Test
    void localVariableWorks() {
        Inspector d = run("LocalVariableTest");
        Inspector.Task f = d.tasks.get(0);
        int yVal = f.altData.get("y").get("val");
        assertThat(yVal).isEqualTo(16);
    }

    @Test
    void usingReferenceInstanceWork() {
        Inspector d = run("UsingReferenceTest");
        byte[] bytes = new byte[4];
        d.load(16392, bytes);
        int fieldX = bytes[0];
        assertThat(fieldX).isEqualTo(99);
    }

    @Test
    void arrayListTest() {
        Inspector d = run("ArrayListTest");
        byte[] bytes = new byte[20];
        d.load(16680, bytes);
        int fieldX = bytes[12];
        assertThat(fieldX).isEqualTo(99);
    }

    @Test
    void listTest() {
        Inspector d = run("ListTest");
        byte[] bytes = new byte[20];
        d.load(16680, bytes);
        int fieldX = bytes[12];
        assertThat(fieldX).isEqualTo(99);
    }

    @Test
    void exceptionTest() {
        Inspector d = run("ExceptionTest");
        Inspector.Task f = d.tasks.get(0);
        int yVal = f.altData.get("x").get("val");
        assertThat(yVal).isEqualTo(10);
    }

    @Test
    void constantsTest() {
        Inspector d = run("ConstantsTest");
        Inspector.Task f = d.tasks.get(0);
        int xVal = f.altData.get("x").get("val");
        int yVal = f.altData.get("y").get("val");
        assertThat(xVal).isEqualTo(67);
        assertThat(yVal).isEqualTo(111);
    }

    @Test
    void clientAndServer() {
        String clientName = "TalkerOne";
        String serverName = "Server";
        Administrator administrator = administrator(clientName, serverName);
        Class server = administrator.classes.get(serverName);
        Class client = administrator.classes.get(clientName);
        int clientInstance = administrator.initClass(client);
        int serverInstance = administrator.initClass(server);

        Administrator.LogicianQuota clientThread = administrator.find(clientInstance).orElseThrow();
        administrator.schedule(clientThread);
        while (clientThread.status != Administrator.Status.Complete) {
            administrator.m.tick();
        }

        Inspector d = administrator.snapshot(0);
        byte[] bytes = new byte[12];
        d.load(16392, bytes);
        assertThat(bytes).isEqualTo("hello client".getBytes());
    }

    @Test
    void clientAndServerWithProtocol() {
        String clientName = "TalkerTwo";
        String serverName = "Server";
        Administrator administrator = administrator(clientName, serverName);
        Class server = administrator.classes.get(serverName);
        Class client = administrator.classes.get(clientName);
        int clientInstance = administrator.initClass(client);
        int serverInstance = administrator.initClass(server);

        Administrator.LogicianQuota clientThread = administrator.find(clientInstance).orElseThrow();
        administrator.schedule(clientThread);
        while (clientThread.status != Administrator.Status.Complete) {
            administrator.m.tick();
            if (administrator.m.processors.get(0).instance == 1 &&
                    administrator.m.processors.get(0).instruction == 21816) {
                administrator.snapshot(1);
            }
        }

        Inspector d = administrator.snapshot(0);
        byte[] bytes = new byte[12];
        d.load(16392, bytes);
        assertThat(bytes).isEqualTo("hello client".getBytes());
    }

    @Test
    void allocatorWorks() {
        Inspector d = run("AllocatorTest");
        Inspector.Task f = d.tasks.get(0);
        int allocateOneVal = f.altData.get("allocateOne").get("val");
        int allocateTwoVal = f.altData.get("allocateTwo").get("val");
        assertThat(allocateOneVal).isEqualTo(16392);
        assertThat(allocateTwoVal).isEqualTo(16516);
    }

    @Test
    void enumWorks() {
        Inspector d = run("EnumTest");
        Inspector.Task f = d.tasks.get(0);
        int xVal = f.altData.get("x").get("val");
        assertThat(xVal).isEqualTo(4);
    }

    @Test
    void methodOverloadWorks() {
        Inspector d = run("MethodOverloadTest");
        Inspector.Task f = d.tasks.get(0);
        int xVal = f.altData.get("x").get("val");
        assertThat(xVal).isEqualTo(10);
    }

    @Test
    void callerArgumentWorks() {
        Inspector d = run("ImplicitArgumentTest");
        Inspector.Task f = d.tasks.get(0);
        int bVal = f.altData.get("b").get("val");
        int cVal = f.altData.get("c").get("val");
        assertThat(bVal).isEqualTo(0);
        assertThat(cVal).isEqualTo(1);
    }

    @Test
    void stringEqualsWorks() {
        Inspector d = run("StringEqualsTest");
        Inspector.Task f = d.tasks.get(0);
        int bVal = f.altData.get("b").get("val");
        assertThat(bVal).isEqualTo(1);
    }

    @Test
    void tmp() {
        Inspector d = run("NewTest");
        Inspector.Task f = d.tasks.get(0);
        int bVal = f.altData.get("x").get("val");
        assertThat(bVal).isEqualTo(14);
    }

    @Test
    void tryWorks() {
        Inspector d = run("TryTest");
        Inspector.Task f = d.tasks.get(0);
        int bVal = f.altData.get("x").get("val");
        assertThat(bVal).isEqualTo(30);
    }

}
