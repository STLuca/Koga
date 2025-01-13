package system;

import core.Class;
import core.Instruction;
import machine.Notifiable;
import machine.Processor;
import machine.VirtualMachine;

import java.util.*;

import static core.Class.Type.*;
import static machine.VirtualMachine.PAGE_SIZE;
import static machine.VirtualMachine.intToBytes;

public class Administrator implements Inspectable, Notifiable {

    final static int MEMBER_OUT_PAGE   = 1;
    final static int MEMBER_IN_PAGE    = 2;
    final static int ROOT_OBJECT_PAGE  = 3;
    final static int ALLOCATOR_PAGE    = 4;
    final static int METHOD_PAGE_START = 5;

    final static int SECONDARY_SIZE = 200;

    VirtualMachine m;

    // Loading
    Map<String, Class> classes = new HashMap<>();
    Map<Class, ClassInstantiateInfo> classInstantiateInfo = new HashMap<>();
    Map<Class, ClassRuntimeValues> runtimeValues = new HashMap<>();

    // Running
    Integer currentInstance = 0;
    Map<Integer, InstanceInfo> instanceInfo = new HashMap<>();
    Map<Class, String> runtimeTableDebug = new HashMap<>();

    // Page allocator
    byte freePage = 1;
    Map<Integer, Integer> pageMapsByObjectId = new HashMap<>();

    // Scheduling
    List<LogicianQuota> threads = new ArrayList<>();
    LogicianQuota scheduled;

    Administrator() {
        this.m = new VirtualMachine(1, this);
    }

    public static Administrator boot() {
        return new Administrator();
    }

    // adding a class
    void integrate(Class c) {
        if (classes.containsKey(c.name)) return;
        classes.put(c.name, c);

        // COMPILE
        Map<String, Integer> constMap = new HashMap<>();
        Map<Class.Method, Integer> methodMap = new HashMap<>();
        Map<Class.Method, Integer> methodEndMap = new HashMap<>();
        List<Byte> bytes = new ArrayList<>();

        for (Class.Method m : c.methods) {
            int start = bytes.size();
            methodMap.put(m, start);

            Map<Integer, Integer> addresses = new HashMap<>();
            int i = 0;
            int curr = 0;
            for (Instruction in : m.instructions) {
                addresses.put(i, curr);
                i++;
                curr += 18;
                // 1 byte for type, subType, inputType, destSize, src1Size, src2Size
                // 4 bytes for dest, src1, src2
                // plan is to make variable size for each instruction, reason for not doing 18 * length
            }

            curr = 0;
            for (Instruction in : m.instructions) {
                bytes.add((byte) in.type.ordinal());
                int inType = switch (in.type) {
                    case Logic -> in.lType.ordinal();
                    case Jump -> in.jType.ordinal();
                    case ConditionalBranch -> in.bType.ordinal();
                    case Class -> in.cmType.ordinal();
                    case Logician -> in.lgType.ordinal();
                    case Memory -> in.mType.ordinal();
                    case Interrupt -> in.iType.ordinal();
                    case Debug -> in.dType.ordinal();
                };
                bytes.add((byte) inType);
                bytes.add((byte) in.inputType.ordinal());

                int dest;
                switch (in.type) {
                    case ConditionalBranch -> {
                        dest = addresses.get(in.dest) - addresses.get(curr);
                    }
                    case Jump -> {
                        switch (in.inputType) {
                            case I -> dest = addresses.get(in.dest) - addresses.get(curr);
                            default -> dest = in.dest;
                        }
                    }
                    default -> dest = in.dest;
                }

                bytes.add((byte) in.destSize);
                for (Byte b : intToBytes(dest)) {
                    bytes.add(b);
                }

                bytes.add((byte) in.src1Size);
                for (Byte b : intToBytes(in.src1)) {
                    bytes.add(b);
                }

                bytes.add((byte) in.src2Size);
                for (Byte b : intToBytes(in.src2)) {
                    bytes.add(b);
                }

                curr++;
            }
            methodEndMap.put(m, bytes.size());
        }
        for (Class.Const constant : c.consts) {
            constMap.put(constant.name, bytes.size());
            for (byte b : constant.value) {
                bytes.add(b);
            }
        }

        // 1. Set up the class pages

        // 1.1 Add the methods
        int pageCount = (bytes.size() / VirtualMachine.PAGE_SIZE) + 1;
        int[] methodPages = new int[pageCount];
        for (int i = 0; i < pageCount; i++) {
            methodPages[i] = allocate();
        }
        int x = 0;
        for (int i = 0; i < pageCount; i++) {
            for (int ii = 0; ii < VirtualMachine.PAGE_SIZE && x < bytes.size(); ii++) {
                m.pages[methodPages[i]][ii] = bytes.get(x);
                x++;
            }
        }

        // 1.2 Create the runtime information
        ClassRuntimeValues v = new ClassRuntimeValues();
        v.size = c.size;
        v.pages = methodPages;
        for (Class.Data data : c.data) {
            v.fields.put(data.name(), new FieldRuntimeValues(data.start()));
        }
        if (c.type == Interface) {
            int indx = 0;
            for (Class.Method method : c.methods) {
                v.methods.put(method.name, new MethodRuntimeValues(0, indx, indx + 1));
                indx += 1;
            }
        } else {
            for (Class.Method method : c.methods) {
                int methodSize = method.size;
                // if (method.requiresAdmin) methodSize += SECONDARY_SIZE;
                v.methods.put(method.name, new MethodRuntimeValues(methodSize, methodMap.get(method), methodEndMap.get(method)));
            }
        }

        for (Class.Const constant : c.consts) {
            v.constants.put(constant.name, new ConstRuntimeValues(constant.value.length, constMap.get(constant.name)));
        }

        // Don't really need to store the runtime value of a system class because we'll make a template now and thus won't use it again
        // but currently use it in the initTemplate method, should pass as an argument
        // if (c.type == Class.Type.Hosted)
        runtimeValues.put(c, v);

        // Only Host classes have a template, Hosted and embedded in other templates
        if (c.type == Class.Type.Host) makeTemplate(c);

    }

    /*
        Expect dependencies to be initialised already i.e. added to classes
        TODO: not recursive
     */
    void resolveDependencies(Class clazz, List<Class> dependencies, Set<Class> resolved) {
        if (resolved.contains(clazz)) return;
        resolved.add(clazz);
        if (!dependencies.contains(clazz)) dependencies.add(clazz);
        for (Class.Symbol s : clazz.symbols) {
            if (s.type != Class.Symbol.Type.CLASS) continue;
            if (s.identifier.equals("Administrator")) continue;
            Class dependency = classes.get(s.identifier);
            resolveDependencies(dependency, dependencies, resolved);
        }
    }

    void makeTemplate(Class c) {
        // Each symbol entry writes a size and address (in order)
        final int SYMBOL_ENTRY_SIZE = 2;
        final int ENTRY_VALUE_SIZE = 4;

        // Get the dependencies
        List<Class> dependencies = new ArrayList<>();
        resolveDependencies(c, dependencies, new HashSet<>());

        Class mAdministrator = dependencies.stream()
                .filter(d -> d.implementing.contains("Administrator"))
                .findFirst()
                .orElse(null);


        Map<String, Class> dependenciesByName = new HashMap<>();
        for (Class d : dependencies) {
            dependenciesByName.put(d.name, d);
        }
        dependenciesByName.put("Administrator", mAdministrator);

        int tableAddress = 0;

        // Calculate each class runtime table address
        Map<Class, Integer> tableEntries = new HashMap<>();
        for (Class dependency : dependencies) {
            if (dependency.type == Interface) continue;
            tableEntries.put(dependency, tableAddress);
            tableAddress += (dependency.symbols.size() * SYMBOL_ENTRY_SIZE);
        }

        // Calculate each interface implementations runtime table address
        Map<Class, Map<Class, Integer>> interfaceTableEntries = new HashMap<>();
        for (Class clazz : dependencies) {
            if (clazz.type != Hosted) continue;
            if (clazz.implementing.isEmpty()) continue;
            Map<Class, Integer> interfaceMap = new HashMap<>();
            interfaceTableEntries.put(clazz, interfaceMap);
            for (String implementingName : clazz.implementing) {
                Class implementing = dependenciesByName.get(implementingName);
                int implementingSize = implementing.methods.size() * SYMBOL_ENTRY_SIZE;
                interfaceMap.put(implementing, tableAddress);
                tableAddress += implementingSize;
            }
        }

        // Calculate each class method relative address
        int tablePagesCount = 1 + (tableAddress * ENTRY_VALUE_SIZE / VirtualMachine.PAGE_SIZE);
        Map<Class, Integer> addressStart = new HashMap<>();
        int currentPage = METHOD_PAGE_START;
        for (Class clazz : dependencies) {
            if (clazz.type == Interface) {
                addressStart.put(clazz, 0);
                continue;
            }
            addressStart.put(clazz, currentPage * VirtualMachine.PAGE_SIZE);
            currentPage += runtimeValues.get(clazz).pages.length;
        }

        // For every dependency class, write its symbols to the runtime table
        RuntimeTable rt = new RuntimeTable();
        for (Class dependency : dependencies) {
            if (dependency.type == Interface) continue;
            rt.clazz(dependency);
            for (Class.Symbol s : dependency.symbols) {
                switch (s.type) {
                    case CLASS -> {
                        Class clazz = dependenciesByName.get(s.identifier);
                        if (clazz.type == Interface) {
                            rt.empty(clazz);
                            continue;
                        }
                        ClassRuntimeValues runtimeValues = this.runtimeValues.get(clazz);
                        int size = runtimeValues.size;
                        int addr = tableEntries.get(clazz) * ENTRY_VALUE_SIZE;
                        rt.clazz(clazz, size, addr);
                    }
                    case METHOD -> {
                        String[] split = s.identifier.split("\\.");
                        Class clazz = dependenciesByName.get(split[0]);
                        ClassRuntimeValues runtimeValues = this.runtimeValues.get(clazz);
                        int size = runtimeValues.methods.get(split[1]).size;
                        int addr = runtimeValues.methods.get(split[1]).addr + addressStart.get(clazz);
                        rt.method(clazz, split[1], size, addr);
                    }
                    case FIELD -> {
                        String[] split = s.identifier.split("\\.");
                        Class clazz = dependenciesByName.get(split[0]);
                        ClassRuntimeValues runtimeValues = this.runtimeValues.get(clazz);
                        int size = 0; // no size saved
                        int addr = runtimeValues.fields.get(split[1]).addr;
                        rt.field(clazz, split[1], size, addr);
                    }
                    case CONST -> {
                        ClassRuntimeValues runtimeValues = this.runtimeValues.get(dependency);
                        int size = runtimeValues.constants.get(s.identifier).size;
                        int addr = runtimeValues.constants.get(s.identifier).addr + addressStart.get(dependency);
                        rt.constant(dependency, s.identifier, size, addr);
                    }
                    case INTERFACE -> {
                        String[] split = s.identifier.split("\\.");
                        Class concreteClazz = dependenciesByName.get(split[0]);
                        Class interfaceClazz = dependenciesByName.get(split[1]);
                        int size = 0; // Interface has no size or size of the table?
                        int address = interfaceTableEntries.get(concreteClazz).get(interfaceClazz) * ENTRY_VALUE_SIZE;
                        rt.intrface(concreteClazz, interfaceClazz, address);
                    }
                    case SYSTEM -> {
                        // Runtime table only uses one page, could be more than 1 page
                        if (s.identifier.equals("SystemIn")) {
                            rt.system(s.identifier, 1);
                        } else if (s.identifier.equals("SystemOut")) {
                            rt.system(s.identifier, 2);
                        } else if (s.identifier.equals("Root")) {
                            rt.system(s.identifier, 3);
                        } else if (s.identifier.equals("Administrator")) {
                            rt.system(s.identifier, 4);
                        }
                    }
                }
            }
        }

        // Write interfaceTableEntries to the runtime table
        for (Class clazz : dependencies) {
            if (clazz.type == Interface) continue;
            if (clazz.implementing.isEmpty()) continue;
            Map<Class, Integer> interfaceMap = new HashMap<>();
            interfaceTableEntries.put(clazz, interfaceMap);
            for (String implementingName : clazz.implementing) {
                Class implementing = dependenciesByName.get(implementingName);
                rt.intrface(clazz, implementing);
                for (Class.Method method : implementing.methods) {
                    ClassRuntimeValues runtimeValues = this.runtimeValues.get(clazz);
                    int size = runtimeValues.methods.get(method.name).size;
                    int addr = runtimeValues.methods.get(method.name).addr + addressStart.get(clazz);
                    rt.method(implementing, method.name, size, addr);
                }
            }
        }

        runtimeTableDebug.put(c, rt.debug.toString());
        // Write runtime table to a free page
        // Only use one page, will break if more than one
        int runtimePage = allocate();
        for (int i = 0; i < rt.table.size(); i++) {
            byte[] bytes = machine.VirtualMachine.intToBytes(rt.table.get(i));
            for (int ii = 0; ii < 4; ii++) {
                m.pages[runtimePage][(i * 4) + ii] = bytes[ii];
            }
        }

        // make a template page using the pageMap, runtime table, and all imported class methods
        List<Integer> templatePage = new ArrayList<>();
        templatePage.add(runtimePage);
        templatePage.add(allocate()); // SystemIn
        templatePage.add(allocate()); // SystemOut
        templatePage.add(allocate()); // Root
        templatePage.add(allocate()); // Allocator
        for (Class clazz : dependencies) {
            if (clazz.type == Interface) continue;
//            if (clazz.type == Protocol) {
//                templatePage.add(0);
//                continue;
//            }
            int[] cPages = runtimeValues.get(clazz).pages;
            for (int cPage : cPages) {
                templatePage.add(cPage);
            }
        }

        byte[] templatePageAsBytes = new byte[templatePage.size()];
        for (int i = 0; i < templatePageAsBytes.length; i++) {
            templatePageAsBytes[i] = (byte) (int) templatePage.get(i);
        }

        ClassInstantiateInfo info = new ClassInstantiateInfo();
        info.pages = templatePageAsBytes;
        // assume the first method
        info.initAddr = VirtualMachine.PAGE_SIZE * METHOD_PAGE_START;
        info.adminTable = tableEntries.get(mAdministrator) * 4;
        classInstantiateInfo.put(c, info);
    }

    int initClass(Class c) {
        if (c.type != Class.Type.Host) throw new RuntimeException();

        // get the template
        ClassInstantiateInfo info = classInstantiateInfo.get(c);

        int objectId = currentInstance++;

        // make a new copy
        int instancePageMap = table(objectId);
        System.arraycopy(info.pages, 0, m.pages[instancePageMap], 0, info.pages.length);

        int taskMetaSize = 20;
        // Use free space at the end of the root object page to set up thread and frame
        int methodSize = taskMetaSize + c.methods.get(0).size + SECONDARY_SIZE;
        int frame = ROOT_OBJECT_PAGE * PAGE_SIZE - methodSize + taskMetaSize;
        int adminTask = frame + methodSize - SECONDARY_SIZE;
        int object = ROOT_OBJECT_PAGE * PAGE_SIZE;
        int adminObject = PAGE_SIZE * ALLOCATOR_PAGE;

        Processor.Snapshot s = new Processor.Snapshot();
        s.instance = objectId;
        s.pageMap = instancePageMap;

        s.interrupted = false;
        s.interruptType = null;
        s.interruptValue = 0;
        s.interruptedBy = 0;

        s.object = object;
        s.table = 0;
        s.instruction = info.initAddr;
        s.task = frame;

        s.altObject = adminObject;
        s.altTable = info.adminTable;
        s.altInstruction = 0;
        s.altTask = adminTask;

        addThread(objectId, instancePageMap, s);
        schedule();

        InstanceInfo instance = new InstanceInfo();
        instance.instance = objectId;
        instance.template = info;
        instance.c = c;
        instance.rootAddr = ROOT_OBJECT_PAGE * PAGE_SIZE;
        instance.pageMap = instancePageMap;
        instance.memberOut = new IO.MemberOut(instancePageMap, MEMBER_OUT_PAGE * PAGE_SIZE, m);
        instance.memberIn = new IO.MemberIn(instancePageMap, MEMBER_IN_PAGE * PAGE_SIZE, m);
        instanceInfo.put(objectId, instance);

        return objectId;
    }

    enum AdminMethod {
        Unknown,
        ConnectRequest,
        ConnectResponse,
        AllocatePage,
        Exit
    }

    // Only works for SystemOut/MemberIn right now
    public void notify(Instruction.InterruptType interruptType, int instance, int interruptValue) {
        InstanceInfo info = instanceInfo.get(instance);
        if (interruptValue != MEMBER_IN_PAGE * PAGE_SIZE) throw new RuntimeException("What port?");
        IO.MemberIn memberIn = info.memberIn;
        int peek = memberIn.peekInt();
        AdminMethod methodType = AdminMethod.values()[peek];
        if (methodType == AdminMethod.Unknown) throw new RuntimeException("Unknown method");
        memberIn.readInt();
        switch (methodType) {
            case ConnectRequest -> {
                int receiverInstance = memberIn.readInt();
                int protocol = memberIn.readInt();
                int method = memberIn.readInt();
                int page1 = memberIn.readInt();
                int page2 = memberIn.readInt();
                int[] pages = new int[2];
                pages[0] = page1;
                pages[1] = page2;
                handleConnectRequest(instance, receiverInstance, protocol, method, pages);
            }
            case ConnectResponse -> {
                if (!info.awaitingResponse) throw new RuntimeException("not awaiting a connection response");
                int pageCount = memberIn.readInt();
                int[] pages = new int[pageCount];
                for (int i = 0; i < pageCount; i++) {
                    pages[i] = memberIn.readInt();
                }
                connect(info.clientInstance, info.pages, info.instance, pages);
            }
            case AllocatePage -> {
                int localPage = memberIn.readInt();
                allocate(instance, localPage);
            }
            case Exit -> {
                m.processors.get(0).snapshot(scheduled.snapshot);
                scheduled.status = Status.Complete;
                schedule();
            }
            default -> throw new RuntimeException("Bad interrupt type");
        }
    }

    void handleConnectRequest(int client, int receiverInstance, int protocol, int method, int[] pages) {
        InstanceInfo receiver = instanceInfo.get(receiverInstance);
        receiver.memberOut.write(2);
        receiver.awaitingResponse = true;
        receiver.clientInstance = client;
        receiver.pages = pages;

        // schedule a thread of the client instance
        LogicianQuota t = find(receiverInstance).orElseThrow();
        schedule(t);
    }

    record Entry(int start, int end) {}
    Inspector snapshot(int instanceId) {
        Administrator a = this;
        Inspector d = new Inspector();
        Map<Entry, Class.Method> methodByInstruction = new HashMap<>();

        Administrator.InstanceInfo info = a.instanceInfo.get(instanceId);
        Administrator.ClassInstantiateInfo instantiateInfo = info.template;

        d.pageMap = info.pageMap;
        d.machine = m;

        // Methods
        // Use class runtime values to create methods

        Inspector.RuntimeClass runtimeClass = new Inspector.RuntimeClass();
        runtimeClass.runtimeTable = a.runtimeTableDebug.get(info.c);

        byte[] pageMap = instantiateInfo.pages;
        int i = METHOD_PAGE_START; // skip runtime table and member components
        loop: while (i < pageMap.length) {
            byte page = pageMap[i];
            for (Administrator.ClassRuntimeValues rvs : a.runtimeValues.values()) {
                if ((byte) rvs.pages[0] != page) continue;
                for (String methodName : rvs.methods.keySet()) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Data:\n");
                    Class methodClass = a.runtimeValues.entrySet().stream().filter(e -> e.getValue() == rvs).map(Map.Entry::getKey).findFirst().orElseThrow();
                    Class.Method method = methodClass.methods.stream().filter(m -> m.name.equals(methodName)).findFirst().orElseThrow();
                    List<Class.Data> orderedData = method.data.stream()
                            .filter(e -> e.name().contains("."))
                            .sorted(Comparator.comparing(Class.Data::start))
                            .toList();
                    for (Class.Data data : orderedData) {
                        int start = data.start();
                        if (start < 10) sb.append(" ");
                        if (start < 100) sb.append(" ");
                        sb.append(start)
                                .append(":")
                                .append(" ")
                                .append(data.name())
                                .append("\n");
                    }

                    sb.append("Instructions:\n");
                    Administrator.MethodRuntimeValues mvs = rvs.methods.get(methodName);
                    int methodAddr = (PAGE_SIZE * (i)) + mvs.addr();
                    int methodEndAddr = (PAGE_SIZE * (i)) + mvs.endAddr();
                    methodByInstruction.put(new Entry(methodAddr, methodEndAddr), method);
                    int curr = methodAddr;
                    while(curr < methodEndAddr) {
                        if (curr < 10) sb.append(" ");
                        if (curr < 100) sb.append(" ");
                        sb.append(curr).append(": ");

                        byte typeIndex = m.loadByte(info.pageMap, curr);
                        byte subType = m.loadByte(info.pageMap, curr + 1);
                        byte inType = m.loadByte(info.pageMap, curr + 2);
                        byte destSize = m.loadByte(info.pageMap, curr + 3);
                        byte dest = m.loadByte(info.pageMap, curr + 4);
                        byte src1Size = m.loadByte(info.pageMap, curr + 8);
                        byte src1 = m.loadByte(info.pageMap, curr + 9);
                        byte src2Size = m.loadByte(info.pageMap, curr + 13);
                        byte src2 = m.loadByte(info.pageMap, curr + 14);

                        Instruction.Type type = Instruction.Type.values()[typeIndex];
                        sb.append(type).append(" ");
                        switch (type) {
                            case Logic -> sb.append(Instruction.LogicType.values()[subType]);
                            case Jump -> sb.append(Instruction.BranchType.values()[subType]);
                            case ConditionalBranch -> sb.append(Instruction.ConditionalBranchType.values()[subType]);
                            case Class -> sb.append(Instruction.ClassType.values()[subType]);
                            case Logician -> sb.append(Instruction.LogicianType.values()[subType]);
                            case Memory -> sb.append(Instruction.MemoryType.values()[subType]);
                            case Interrupt -> sb.append(Instruction.InterruptType.values()[subType]);
                            case Debug -> sb.append(Instruction.DebugType.values()[subType]);
                        }
                        sb.append(" ")
                                .append(Instruction.InputType.values()[inType])
                                .append(" dest(")
                                .append(dest)
                                .append(":")
                                .append(destSize)
                                .append(") src1(")
                                .append(src1)
                                .append(":")
                                .append(src1Size)
                                .append(") src2(")
                                .append(src2)
                                .append(":")
                                .append(src2Size)
                                .append(")");

                        sb.append("\n");

                        curr += 18;
                    }

                    runtimeClass.methods.put(methodClass.name + "." + methodName, sb.toString());
                }

                i+=rvs.pages.length;
                continue loop;
            }
        }
        d.runtimeClasses = runtimeClass;

        for (Processor p : m.processors) {
            Inspector.Task t = new Inspector.Task();
            t.task = p.task;
            t.object = p.object;
            t.table = p.table;
            t.instruction = p.instruction;
            t.altTask = p.altTask;
            t.altObject = p.altObject;
            t.altTable = p.altTable;
            t.altInstruction = p.altInstruction;

            Entry entry = methodByInstruction.keySet().stream()
                    .filter(e -> t.instruction == e.start || (t.instruction > e.start && t.instruction <= e.end))
                    .findFirst().orElse(null);
            Class.Method method = methodByInstruction.get(entry);

            for (Class.Data data : method.data) {
                if (data.name().contains(".") && data.size() <= 4) {
                    String[] split = data.name().split("\\.");
                    t.data.putIfAbsent(split[0], new HashMap<>());
                    t.data.get(split[0]).put(split[1], m.loadInt(info.pageMap, p.task + data.start(), data.size()));
                }
            }
            d.processorTasks.add(t);
        }

        // Tasks (latest snapshots per member)
        for (LogicianQuota thread : threads) {
            if (thread.instance != instanceId) continue;

            Inspector.Task t = new Inspector.Task();
            t.task = thread.snapshot.task;
            t.object = thread.snapshot.task;
            t.table = thread.snapshot.table;
            t.instruction = thread.snapshot.instruction;

            t.altTask = thread.snapshot.altTask;
            t.altObject = thread.snapshot.altObject;
            t.altTable = thread.snapshot.altTable;
            t.altInstruction = thread.snapshot.altInstruction;

            methodByInstruction.keySet().stream()
                    .filter(e -> t.instruction > e.start && t.instruction <= e.end)
                    .findFirst()
                    .ifPresent(entry -> {
                        Class.Method method = methodByInstruction.get(entry);
                        for (Class.Data data : method.data) {
                            if (data.name().contains(".") && data.size() <= 4) {
                                String[] split = data.name().split("\\.");
                                t.data.putIfAbsent(split[0], new HashMap<>());
                                t.data.get(split[0]).put(split[1], m.loadInt(info.pageMap, t.task + data.start(), data.size()));
                            }
                        }
                    });

            methodByInstruction.keySet().stream()
                    .filter(e -> t.altInstruction > e.start && t.altInstruction <= e.end)
                    .findFirst()
                    .ifPresent(entry -> {
                        Class.Method method = methodByInstruction.get(entry);
                        for (Class.Data data : method.data) {
                            if (data.name().contains(".") && data.size() <= 4) {
                                String[] split = data.name().split("\\.");
                                t.altData.putIfAbsent(split[0], new HashMap<>());
                                t.altData.get(split[0]).put(split[1], m.loadInt(info.pageMap, t.altTask + data.start(), data.size()));
                            }
                        }
                    });

            d.tasks.add(t);
        }

        return d;
    }

    // Page allocator

    // makes a new page map for the object (1 per object)
    int table(int objectId) {
        if (pageMapsByObjectId.containsKey(objectId)) {
            return pageMapsByObjectId.get(objectId);
        }
        int next = allocate();
        pageMapsByObjectId.put(objectId, next);
        return next;
    }

    // Sets a free page to the local page in the objects page map
    int allocate(int objectId, int localPage) {
        int pageMap = pageMapsByObjectId.get(objectId);
        byte[] page = m.pages[pageMap];
        int next = allocate();
        page[localPage] = (byte) next;
        return next;
    }

    // Used by the system to make runtime/method pages
    int allocate() {
        return freePage++;
    }

    void connect(int client, int[] clientPages, int server, int[] serverPages) {
        byte[] clientPageMap = m.pages[pageMapsByObjectId.get(client)];
        byte[] serverPageMap = m.pages[pageMapsByObjectId.get(server)];
        for (int i = 0; i < clientPages.length; i++) {
            serverPageMap[serverPages[i] / VirtualMachine.PAGE_SIZE] = clientPageMap[clientPages[i] / VirtualMachine.PAGE_SIZE];
        }
    }

    // Scheduling

    LogicianQuota addThread(int instance, int pageMap, Processor.Snapshot snapshot) {
        LogicianQuota t = new LogicianQuota();
        t.instance = instance;
        t.pageMap = pageMap;
        t.status = Status.Incomplete;
        t.snapshot = snapshot;
        threads.add(t);
        return t;
    }

    Optional<LogicianQuota> find(int instance) {
        return threads.stream().filter(t -> t.instance == instance)
                .findFirst();
    }

    void schedule() {
        for (LogicianQuota t : threads) {
            if (t.status != Status.Complete) {
                schedule(t);
                return;
            }
        }
    }

    void schedule(LogicianQuota t) {
        if (this.scheduled != null) {
            m.processors.get(0).snapshot(scheduled.snapshot);
        }
        m.processors.get(0).instance = t.instance;
        m.processors.get(0).pageMap = t.pageMap;
        m.processors.get(0).load(t.snapshot);
        scheduled = t;
    }

    //
    record FieldRuntimeValues (int addr                       ) {}
    record ConstRuntimeValues (int size, int addr             ) {}
    record MethodRuntimeValues(int size, int addr, int endAddr) {}
    static class ClassRuntimeValues {
        int[] pages;
        int size;
        Map<String, MethodRuntimeValues> methods = new HashMap<>();
        Map<String, FieldRuntimeValues> fields = new HashMap<>();
        Map<String, ConstRuntimeValues> constants = new HashMap<>();
    }

    // Data needed to instantiate a new class
    static class ClassInstantiateInfo {
        byte[] pages;
        int initAddr;
        int adminTable;
    }

    // Data about instances
    static class InstanceInfo {
        int instance;
        int pageMap;
        int rootAddr;
        IO.MemberOut memberOut;
        IO.MemberIn memberIn;
        ClassInstantiateInfo template;
        Class c;

        boolean awaitingResponse = false;
        int clientInstance;
        int[] pages;
    }

    enum Status {
        Incomplete, Complete
    }
    static class LogicianQuota {
        int instance;
        int pageMap;
        Status status;
        Processor.Snapshot snapshot;
    }

}
