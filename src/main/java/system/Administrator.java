package system;

import core.Types;
import machine.Notifiable;
import machine.Processor;
import machine.VirtualMachine;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.*;

import static machine.VirtualMachine.PAGE_SIZE;
import static machine.VirtualMachine.intToBytes;

public class Administrator implements Notifiable {

    final static int MEMBER_OUT_PAGE   = 1;
    final static int MEMBER_IN_PAGE    = 2;
    final static int ROOT_OBJECT_PAGE  = 3;
    final static int ALLOCATOR_PAGE    = 4;
    final static int HOSTED_START      = 5;

    final static int SECONDARY_SIZE = 200;
    final static int INSTRUCTION_SIZE = 18;

    VirtualMachine m;

    // Loading
    HashMap<String, Document> documents = new HashMap<>();
    HashMap<String, HostInfo> hostInfo = new HashMap<>();
    HashMap<String, HostedInfo> hostedInfo = new HashMap<>();
    HashMap<String, InterfaceInfo> interfaceInfo = new HashMap<>();
    HashMap<String, ProtocolInfo> protocolInfo = new HashMap<>();
    int nextProtocolMethodId = 3;

    // Running
    // Hosts
    Integer currentInstance = 0;
    HashMap<Integer, Administrator.Host> hosts = new HashMap<>();

    // Page allocator
    byte freePage = 1;

    // Scheduling
    ArrayList<LogicianQuota> quotas = new ArrayList<>();
    LogicianQuota scheduled;

    Administrator() {
        this.m = new VirtualMachine(1, this);
    }

    public static Administrator boot() {
        return new Administrator();
    }

    // Compiler

    int[] compile(DataInputStream in, HashMap<String, MethodRuntimeValues> methods) throws IOException {
        int methodCount = in.readInt();
        int totalInstructionCount = in.readInt();
        int methodPageCount = 1 + (totalInstructionCount * INSTRUCTION_SIZE / PAGE_SIZE);
        int[] methodPages = allocate(methodPageCount);
        int byteIdx = 0;
        for (int i = 0; i < methodCount; i++) {
            MethodRuntimeValues mrv = new MethodRuntimeValues();
            String methodName = in.readUTF();
            mrv.name = methodName;
            int methodSize = in.readInt();
            int paramsCount = in.readInt();
            for (int pi = 0; pi < paramsCount; pi++) {
                in.readUTF();
            }
            int methodDataCount = in.readInt();
            for (int di = 0; di < methodDataCount; di++) {
                String dataName = in.readUTF();
                int dataAddr = in.readInt();
                int dataSize = in.readInt();
                DataRuntimeValue d = new DataRuntimeValue(dataName, dataSize, dataAddr);
                mrv.data.put(dataName, d);
            }
            int instructionCount = in.readInt();

            int endAddr = byteIdx + instructionCount * INSTRUCTION_SIZE;
            mrv.size = methodSize;
            mrv.addr = byteIdx;
            mrv.endAddr = endAddr;
            methods.put(mrv.name, mrv);

            int curr = 0;
            for (int ii = 0; ii < instructionCount; ii++) {
                int inType = in.readInt();
                int subType = in.readInt();
                int inputType = in.readInt();
                this.m.pages[methodPages[byteIdx / PAGE_SIZE]][byteIdx % PAGE_SIZE] = (byte) inType;
                byteIdx++;
                this.m.pages[methodPages[byteIdx / PAGE_SIZE]][byteIdx % PAGE_SIZE] = (byte) subType;
                byteIdx++;
                this.m.pages[methodPages[byteIdx / PAGE_SIZE]][byteIdx % PAGE_SIZE] = (byte) inputType;
                byteIdx++;

                int src1Size = in.readInt();
                int src1 = in.readInt();
                int src2Size = in.readInt();
                int src2 = in.readInt();
                int src3Size = in.readInt();
                int src3 = in.readInt();
                switch (Types.Instruction.values()[inType]) {
                    case ConditionalBranch -> {
                        src1 = INSTRUCTION_SIZE * (src1 - curr);
                    }
                    case Jump -> {
                        switch (Types.InputType.values()[inputType]) {
                            case I -> {
                                src1 = INSTRUCTION_SIZE * (src1 - curr);
                                src2 = src1;
                            }
                        }
                    }
                }

                this.m.pages[methodPages[byteIdx / PAGE_SIZE]][byteIdx % PAGE_SIZE] = (byte) src1Size;
                byteIdx++;
                for (Byte b : intToBytes(src1)) {
                    this.m.pages[methodPages[byteIdx / PAGE_SIZE]][byteIdx % PAGE_SIZE] = b;
                    byteIdx++;
                }

                this.m.pages[methodPages[byteIdx / PAGE_SIZE]][byteIdx % PAGE_SIZE] = (byte) src2Size;
                byteIdx++;
                for (Byte b : intToBytes(src2)) {
                    this.m.pages[methodPages[byteIdx / PAGE_SIZE]][byteIdx % PAGE_SIZE] = b;
                    byteIdx++;
                }

                this.m.pages[methodPages[byteIdx / PAGE_SIZE]][byteIdx % PAGE_SIZE] = (byte) src3Size;
                byteIdx++;
                for (Byte b : intToBytes(src3)) {
                    this.m.pages[methodPages[byteIdx / PAGE_SIZE]][byteIdx % PAGE_SIZE] = b;
                    byteIdx++;
                }

                curr++;
            }
        }
        return methodPages;
    }

    // adding a class
    void integrate(byte[] docBytes) {
        try(ByteArrayInputStream bis = new ByteArrayInputStream(docBytes);
            DataInputStream in = new DataInputStream(bis)) {

            String koga = in.readUTF();
            if (!koga.equals("古河市")) {
                throw new RuntimeException();
            }
            int version = in.readInt();
            String name = in.readUTF();

            if (documents.containsKey(name)) return;
            Document iDoc = new Document();
            documents.put(name, iDoc);
            iDoc.name = name;

            Types.Document docType = Types.Document.values()[in.readInt()];
            switch (docType) {
                case Interface -> {
                    InterfaceInfo interfaceInfo = new InterfaceInfo();
                    interfaceInfo.name = name;
                    int methodsCount = in.readInt();
                    for (int i = 0; i < methodsCount; i++) {
                        String methodName = in.readUTF();
                        interfaceInfo.methods.add(methodName);
                    }
                    this.interfaceInfo.put(name, interfaceInfo);

                    interfaceInfo.doc = iDoc;
                    iDoc.type = Types.Document.Interface;
                    iDoc.interfaceInfo = interfaceInfo;
                }
                case Protocol -> {
                    ProtocolInfo pInfo = new ProtocolInfo();
                    pInfo.name = name;
                    int currentAddr = 4;
                    int protocolMethodsCount = in.readInt();
                    for (int i = 0; i < protocolMethodsCount; i++) {
                        String protocolMethodName = in.readUTF();
                        int portCount = in.readInt();
                        ProtocolMethodInfo pmInfo = new ProtocolMethodInfo();
                        pmInfo.name = protocolMethodName;
                        pmInfo.id = nextProtocolMethodId++;
                        pmInfo.addr = currentAddr;
                        pmInfo.ports = new Port[portCount];
                        for (int pi = 0; pi < portCount; pi++) {
                            int size = in.readInt();
                            int permission = in.readInt();
                            Port p = new Port();
                            p.size = size;
                            p.permissions = permission;
                            pmInfo.ports[pi] = p;
                        }
                        pInfo.methods.put(protocolMethodName, pmInfo);
                    }

                    int byteCount = 0;
                    for (ProtocolMethodInfo pm : pInfo.methods.sequencedValues()) {
                        byteCount += 16 + pm.ports.length * 8;
                    }
                    byte[] bytes = new byte[byteCount];

                    int byteIndex = 0;
                    int currentSymbol = 0;
                    for (Byte b : intToBytes(pInfo.methods.size())) {
                        bytes[byteIndex++] = b;
                    }
                    for (ProtocolMethodInfo pm : pInfo.methods.sequencedValues()) {
                        for (Byte b : intToBytes(pm.id)) {
                            bytes[byteIndex++] = b;
                        }
                        for (Byte b : intToBytes(currentSymbol++)) {
                            bytes[byteIndex++] = b;
                        }
                        for (Byte b : intToBytes(pm.ports.length)) {
                            bytes[byteIndex++] = b;
                        }
                        for (Port port : pm.ports) {
                            for (Byte b : intToBytes(port.size)) {
                                bytes[byteIndex++] = b;
                            }
                            for (Byte b : intToBytes(port.permissions)) {
                                bytes[byteIndex++] = b;
                            }
                        }
                    }

                    int pageSize = 1 + bytes.length / VirtualMachine.PAGE_SIZE;
                    int[] pages = allocate(pageSize);
                    int b = 0;
                    for (int i = 0; i < pageSize; i++) {
                        for (int ii = 0; ii < PAGE_SIZE && b < bytes.length; ii++) {
                            m.pages[pages[i]][ii] = bytes[b++];
                        }
                    }

                    pInfo.pages = pages;
                    protocolInfo.put(name, pInfo);

                    iDoc.type = Types.Document.Protocol;
                    iDoc.protocolInfo = pInfo;
                }
                case Hosted -> {
                    // Create the runtime information
                    HostedInfo hostedInfo = new HostedInfo();
                    hostedInfo.name = name;

                    int size = in.readInt();
                    hostedInfo.size = size;

                    // dependencies
                    int dependencyCount = in.readInt();
                    for (int i = 0; i < dependencyCount; i++) {
                        hostedInfo.dependencies.add(in.readUTF());
                    }

                    // symbols
                    int symbolCount = in.readInt();
                    for (int i = 0; i < symbolCount; i++) {
                        int symbolType = in.readInt();
                        String identifier = in.readUTF();
                        Symbol symbol = new Symbol(Types.Symbol.values()[symbolType], identifier);
                        hostedInfo.symbols.add(symbol);
                    }

                    // constants
                    int constantsCount = in.readInt();
                    int constantsByteSize = in.readInt();
                    int byteIdx = 0;
                    int constPageCount = constantsByteSize == 0 ? 0 : 1 + (constantsByteSize / PAGE_SIZE);
                    int[] constPages = allocate(constPageCount);
                    for (int ci = 0; ci < constantsCount; ci++) {
                        String constName = in.readUTF();
                        int constantLength = in.readInt();
                        byte[] constantValue = in.readNBytes(constantLength);
                        hostedInfo.constants.put(constName, new ConstRuntimeValues(constantLength, byteIdx));
                        for (byte b : constantValue) {
                            int pi = byteIdx / PAGE_SIZE; // page index
                            int bi = byteIdx % PAGE_SIZE; // byte index in the page
                            m.pages[constPages[pi]][bi] = b;
                            byteIdx++;
                        }
                    }
                    hostedInfo.constPages = constPages;

                    // Implementing
                    int implementingCount = in.readInt();
                    for (int i = 0; i < implementingCount; i++) {
                        hostedInfo.implementing.add(in.readUTF());
                    }

                    // Data
                    int dataCount = in.readInt();
                    for (int i = 0; i < dataCount; i++) {
                        String dataName = in.readUTF();
                        int dataStart = in.readInt();
                        int dataSize = in.readInt();
                        hostedInfo.fields.put(dataName, new FieldRuntimeValues(dataStart));
                    }

                    // Methods
                    int[] methodPages = compile(in, hostedInfo.methods);
                    hostedInfo.methodPages = methodPages;

                    this.hostedInfo.put(name, hostedInfo);
                    iDoc.type = Types.Document.Hosted;
                    iDoc.hosted = hostedInfo;
                }
                case Host -> {
                    //  COMPILE
                    //      Methods -> bytes
                    //      Constants -> bytes
                    //      Get linking information e.g. size, addr of classes, fields, methods, consts
                    //  RUNTIME TABLE
                    //      Aggregate host and hosted runtime tables into one
                    //      Fill in the values
                    //          Need to know the address of everything, which depends on the order its added
                    //          Make a class for HostLayout?
                    //  MAKE TEMPLATE
                    //      Runtime table + constants + methods
                    //      Logician values for first method e.g. addr, taskAddr (currently done in init, move here)

                    // Create the runtime information
                    HostInfo hostInfo = new HostInfo();
                    this.hostInfo.put(name, hostInfo);
                    hostInfo.name = name;

                    int hostedSize = in.readInt();
                    hostInfo.size = hostedSize;

                    String administrator = in.readUTF();

                    int dependenciesCount = in.readInt();
                    String[] hostDependencies = new String[dependenciesCount];
                    for (int i = 0; i < dependenciesCount; i++) {
                        hostDependencies[i] = in.readUTF();
                    }

                    // symbols
                    int symbolCount = in.readInt();
                    for (int i = 0; i < symbolCount; i++) {
                        int symbolType = in.readInt();
                        String identifier = in.readUTF();
                        Symbol symbol = new Symbol(Types.Symbol.values()[symbolType], identifier);
                        hostInfo.symbols.add(symbol);
                    }

                    // constants
                    int constantsCount = in.readInt();
                    int constantsByteSize = in.readInt();
                    int byteIdx = 0;
                    int constPageCount = constantsByteSize == 0 ? 0 : 1 + (constantsByteSize / PAGE_SIZE);
                    int[] constPages = allocate(constPageCount);
                    for (int ci = 0; ci < constantsCount; ci++) {
                        String constName = in.readUTF();
                        int constantLength = in.readInt();
                        byte[] constantValue = in.readNBytes(constantLength);
                        hostInfo.constants.put(constName, new ConstRuntimeValues(constantLength, byteIdx));
                        for (byte b : constantValue) {
                            int pi = byteIdx / PAGE_SIZE; // page index
                            int bi = byteIdx % PAGE_SIZE; // byte index in the page
                            m.pages[constPages[pi]][bi] = b;
                            byteIdx++;
                        }
                    }
                    hostInfo.constPages = constPages;

                    // Supporting
                    int supportingCount = in.readInt();
                    for (int i = 0; i < supportingCount; i++) {
                        in.readUTF();
                    }

                    // Data
                    int dataCount = in.readInt();
                    for (int i = 0; i < dataCount; i++) {
                        String dataName = in.readUTF();
                        int dataStart = in.readInt();
                        int dataSize = in.readInt();
                        hostInfo.fields.put(dataName, new FieldRuntimeValues(dataStart));
                    }

                    // Methods
                    int[] methodPages = compile(in, hostInfo.methods);
                    hostInfo.methodPages = methodPages;

                    iDoc.type = Types.Document.Host;
                    iDoc.host = hostInfo;

                    // Each symbol entry writes a size and address (in order)
                    final int SYMBOL_ENTRY_SIZE = 2;
                    final int ENTRY_VALUE_SIZE = 4;

                    // Get the dependencies
                    // Expect dependencies to be initialised already i.e. added to classes
                    LinkedHashSet<Document> dependencies = new LinkedHashSet<>();
                    ArrayDeque<Document> toResolve = new ArrayDeque<>();
                    Document iHostAdmin = null;

                    toResolve.add(iDoc);
                    for (String dependency : hostDependencies) {
                        Document iDocument = documents.get(dependency);
                        toResolve.add(iDocument);
                    }

                    while (!toResolve.isEmpty()) {
                        Document resolve = toResolve.pop();
                        if (dependencies.contains(resolve)) continue;
                        dependencies.add(resolve);
                        if (resolve.name.equals(administrator)) {
                            iHostAdmin = resolve;
                        }
                        switch (resolve.type) {
                            case Hosted -> {
                                for (String dependencyName : resolve.hosted.dependencies) {
                                    Document dependency = documents.get(dependencyName);
                                    toResolve.push(dependency);
                                }
                            }
                            case Host, Protocol, Interface -> {
                                // Don't have dependencies to add
                            }
                        }
                    }

                    if (iHostAdmin == null) {
                        throw new RuntimeException("No admin");
                    }


                    ArrayList<ProtocolInfo> protocolDependencies = new ArrayList<>();
                    for (Document protocolDependency : dependencies) {
                        if (protocolDependency.type == Types.Document.Protocol) {
                            protocolDependencies.add(protocolDependency.protocolInfo);
                        }
                    }

                    int[][] protocolPages = new int[protocolDependencies.size()][];
                    for (int i = 0; i < protocolDependencies.size(); i++) {
                        ProtocolInfo pInfo = protocolDependencies.get(i);
                        protocolPages[i] = pInfo.pages;
                    }
                    hostInfo.protocolPages = protocolPages;

                    // Calculate each class runtime table address
                    int tableAddress = 0;
                    HashMap<Document, Map<Document, Integer>> documentTableEntries = new HashMap<>();
                    for (Document dependency : dependencies) {
                        Document iDocument = documents.get(dependency.name);
                        switch (iDocument.type) {
                            case Interface -> {}
                            case Protocol -> {}
                            case Host -> {
                                documentTableEntries.put(iDocument, new HashMap<>());
                                Map<Document, Integer> tableEntries = documentTableEntries.get(iDocument);
                                tableEntries.put(iDocument, tableAddress);
                                tableAddress += (dependency.host.symbols.size() * SYMBOL_ENTRY_SIZE);
                                for (InterfaceInfo implementing : dependency.host.implementing) {
                                    int implementingSize = implementing.methods.size() * SYMBOL_ENTRY_SIZE;
                                    tableEntries.put(implementing.doc, tableAddress);
                                    tableAddress += implementingSize;
                                }
                            }
                            case Hosted -> {
                                documentTableEntries.put(iDocument, new HashMap<>());
                                Map<Document, Integer> tableEntries = documentTableEntries.get(iDocument);
                                tableEntries.put(iDocument, tableAddress);
                                tableAddress += (dependency.hosted.symbols.size() * SYMBOL_ENTRY_SIZE);
                                for (String implementingName : dependency.hosted.implementing) {
                                    Document implementing = documents.get(implementingName);
                                    int implementingSize = implementing.interfaceInfo.methods.size() * SYMBOL_ENTRY_SIZE;
                                    tableEntries.put(implementing, tableAddress);
                                    tableAddress += implementingSize;
                                }
                            }
                        }
                    }

                    // Calculate each class method table address (address the classes methods start at)
                    HashMap<Document, HostedValues> hostedValuesMap = new HashMap<>();
                    int currentPage = HOSTED_START;
                    for (Document dependency : dependencies) {
                        Document iDocument = documents.get(dependency.name);
                        switch (iDocument.type) {
                            case Interface -> {}
                            case Protocol -> {
                                currentPage += iDocument.protocolInfo.methods.size();
                            }
                            case Hosted -> {
                                HostedInfo hostedInfo = iDocument.hosted;
                                HostedValues hv = new HostedValues();
                                hv.name = dependency.name;
                                hv.constantsStartAddr = currentPage * VirtualMachine.PAGE_SIZE;
                                currentPage += hostedInfo.constPages.length;
                                hv.methodStartAddr = currentPage * VirtualMachine.PAGE_SIZE;
                                currentPage += hostedInfo.methodPages.length;
                                hostedValuesMap.put(iDocument, hv);
                                hv.methodPages = hostedInfo.methodPages;
                                hv.methods = hostedInfo.methods;
                                hostInfo.hostedValues.put(dependency.name, hv);
                            }
                            case Host -> {
                                HostedValues hv = new HostedValues();
                                hv.name = dependency.name;
                                hv.constantsStartAddr = currentPage * VirtualMachine.PAGE_SIZE;
                                currentPage += hostInfo.constPages.length;
                                for (ProtocolInfo protocolDependency : protocolDependencies) {
                                    hv.protocolsAddresses.put(protocolDependency.name, currentPage * VirtualMachine.PAGE_SIZE);
                                    currentPage += protocolDependency.pages.length;
                                }
                                hv.methodStartAddr = currentPage * VirtualMachine.PAGE_SIZE;
                                currentPage += hostInfo.methodPages.length;
                                hostedValuesMap.put(iDocument, hv);
                                hv.methodPages = hostInfo.methodPages;
                                hv.methods = hostInfo.methods;
                                hostInfo.hostedValues.put(dependency.name, hv);
                            }
                        }
                    }

                    // For every dependency class, write its symbols to the runtime table
                    RuntimeTable.Builder rtb = RuntimeTable.builder();
                    for (Document dependency : dependencies) {
                        switch (dependency.type) {
                            case Interface -> {}
                            case Protocol -> {
                                rtb.table(RuntimeTable.TableType.Document, dependency.name);
                                for (ProtocolMethodInfo pm : dependency.protocolInfo.methods.values()) {
                                    if (hostInfo.methods.containsKey(pm.name)) {
                                        int addr = hostInfo.methods.get(pm.name).addr;
                                        int size = hostInfo.methods.get(pm.name).size;
                                        rtb.entry(RuntimeTable.EntryType.Method, hostInfo.name + "." + pm.name, size, addr);
                                    } else {
                                        rtb.entry(RuntimeTable.EntryType.Method, hostInfo.name + "." + pm.name, 0, 0);
                                    }
                                }
                            }
                            case Host, Hosted -> {
                                rtb.table(RuntimeTable.TableType.Document, dependency.name);
                                ArrayList<Symbol> symbols;
                                if (dependency.type == Types.Document.Host) {
                                    symbols = dependency.host.symbols;
                                } else {
                                    symbols = dependency.hosted.symbols;
                                }
                                for (Symbol s : symbols) {
                                    switch (s.type) {
                                        case CLASS -> {
                                            Document document = documents.get(s.identifier);
                                            switch (document.type) {
                                                case Protocol, Interface -> {
                                                    rtb.entry(RuntimeTable.EntryType.Error, "BAD SYMBOL - FIX ", 0, 0);
                                                }
                                                case Hosted -> {
                                                    HostedInfo runtimeValues = document.hosted;
                                                    int size = runtimeValues.size;
                                                    int addr = documentTableEntries.get(document).get(document) * ENTRY_VALUE_SIZE;
                                                    rtb.entry(RuntimeTable.EntryType.Document, document.name, size, addr);
                                                }
                                                case Host -> {
                                                    HostInfo runtimeValues = document.host;
                                                    int size = runtimeValues.size;
                                                    int addr = documentTableEntries.get(document).get(document) * ENTRY_VALUE_SIZE;
                                                    rtb.entry(RuntimeTable.EntryType.Document, document.name, size, addr);
                                                }
                                            }
                                        }
                                        case METHOD -> {
                                            String[] split = s.identifier.split(" ");
                                            String methodName = split[1];
                                            String documentName = split[0];
                                            Document iDocument = documents.get(documentName);
                                            if (iDocument.name.equals("core.Administrator")) {
                                                iDocument = iHostAdmin;
                                            }
                                            switch (iDocument.type) {
                                                case Host -> {
                                                    HostInfo runtimeValues = iDocument.host;
                                                    int size = runtimeValues.methods.get(methodName).size;
                                                    int addr = runtimeValues.methods.get(methodName).addr + hostedValuesMap.get(iDocument).methodStartAddr;
                                                    rtb.entry(RuntimeTable.EntryType.Method, iDocument.name + "." + methodName, size, addr);
                                                }
                                                case Hosted -> {
                                                    HostedInfo runtimeValues = iDocument.hosted;
                                                    int size = runtimeValues.methods.get(methodName).size;
                                                    int addr = runtimeValues.methods.get(methodName).addr + hostedValuesMap.get(iDocument).methodStartAddr;
                                                    rtb.entry(RuntimeTable.EntryType.Method, iDocument.name + "." + methodName, size, addr);
                                                }
                                                case Interface -> {
                                                    InterfaceInfo interfaceInfo = iDocument.interfaceInfo;
                                                    int addr = interfaceInfo.methods.indexOf(methodName);
                                                    rtb.entry(RuntimeTable.EntryType.Method, iDocument.name + "." + methodName, 0, addr);
                                                }
                                                case Protocol -> {
                                                    ProtocolInfo pInfo = iDocument.protocolInfo;
                                                    ProtocolMethodInfo pmInfo = pInfo.methods.get(methodName);
                                                    int protocolAddr = hostInfo.hostedValues.get(name).protocolsAddresses.get(pInfo.name);
                                                    int pMethodAddr = pmInfo.addr;
                                                    rtb.entry(RuntimeTable.EntryType.ProtocolMethod, s.identifier, pmInfo.id, protocolAddr + pMethodAddr);;
                                                }
                                            }
                                        }
                                        case FIELD -> {
                                            String[] split = s.identifier.split(" ");
                                            String fieldName = split[1];
                                            String documentName = split[0];
                                            Document hostedDoc = documents.get(documentName);
                                            switch (hostedDoc.type) {
                                                case Hosted -> {
                                                    HostedInfo runtimeValues = hostedDoc.hosted;
                                                    int size = 0; // no size saved
                                                    int addr = runtimeValues.fields.get(fieldName).addr;
                                                    rtb.entry(RuntimeTable.EntryType.Field, hostedDoc.name + "." + fieldName, size, addr);
                                                }
                                                case Host -> {
                                                    HostInfo runtimeValues = hostedDoc.host;
                                                    int size = 0; // no size saved
                                                    int addr = runtimeValues.fields.get(fieldName).addr;
                                                    rtb.entry(RuntimeTable.EntryType.Field, hostedDoc.name + "." + fieldName, size, addr);
                                                }
                                                case Protocol, Interface -> throw new RuntimeException("Field doesn't exist for type");
                                            }
                                        }
                                        case CONST -> {
                                            Document iDocument = this.documents.get(dependency.name);
                                            switch (iDocument.type) {
                                                case Hosted -> {
                                                    HostedInfo runtimeValues = iDocument.hosted;
                                                    int size = runtimeValues.constants.get(s.identifier).size;
                                                    int addr = runtimeValues.constants.get(s.identifier).addr + hostedValuesMap.get(iDocument).constantsStartAddr;
                                                    rtb.entry(RuntimeTable.EntryType.Constant, s.identifier, size, addr);
                                                }
                                                case Host -> {
                                                    HostInfo runtimeValues = iDocument.host;
                                                    int size = runtimeValues.constants.get(s.identifier).size;
                                                    int addr = runtimeValues.constants.get(s.identifier).addr + hostedValuesMap.get(iDocument).constantsStartAddr;
                                                    rtb.entry(RuntimeTable.EntryType.Constant, s.identifier, size, addr);
                                                }
                                            }
                                        }
                                        case INTERFACE -> {
                                            String[] split = s.identifier.split(" ");
                                            String iName = split[1];
                                            String documentName = split[0];
                                            Document concreteDocument = documents.get(documentName);
                                            Document interfaceDocument = documents.get(iName);
                                            int address = documentTableEntries.get(concreteDocument).get(interfaceDocument) * ENTRY_VALUE_SIZE;
                                            rtb.entry(RuntimeTable.EntryType.Interface, concreteDocument.name + "." + interfaceDocument.name, 0, address);
                                        }
                                        case SYSTEM -> {
                                            // Runtime table only uses one page, could be more than 1 page
                                            // do I still need all these?
                                            switch (s.identifier) {
                                                case "SystemIn" -> {
                                                    rtb.entry(RuntimeTable.EntryType.System, s.identifier, VirtualMachine.PAGE_SIZE, 1 * VirtualMachine.PAGE_SIZE);
                                                }
                                                case "SystemOut" -> {
                                                    rtb.entry(RuntimeTable.EntryType.System, s.identifier, VirtualMachine.PAGE_SIZE, 2 * VirtualMachine.PAGE_SIZE);
                                                }
                                                case "Root" -> {
                                                    rtb.entry(RuntimeTable.EntryType.System, s.identifier, VirtualMachine.PAGE_SIZE, 3 * VirtualMachine.PAGE_SIZE);
                                                }
                                                case "Administrator" -> {
                                                    rtb.entry(RuntimeTable.EntryType.System, s.identifier, VirtualMachine.PAGE_SIZE, 4 * VirtualMachine.PAGE_SIZE);
                                                }
                                            }
                                        }
                                        case PROTOCOLS, PROTOCOL -> {

                                        }
                                    }
                                }

                                switch (dependency.type) {
                                    case Host -> {
                                        for (InterfaceInfo implemented : dependency.host.implementing) {
                                            rtb.table(RuntimeTable.TableType.Interface, dependency.name + "." + implemented.name);
                                            for (String method : implemented.methods) {
                                                HostInfo runtimeValues = dependency.host;
                                                int size = runtimeValues.methods.get(method).size;
                                                int addr = runtimeValues.methods.get(method).addr + hostedValuesMap.get(dependency).methodStartAddr;
                                                rtb.entry(RuntimeTable.EntryType.Method, implemented.name + "." + method, size, addr);
                                            }
                                        }
                                    }
                                    case Hosted -> {
                                        for (String implementingName : dependency.hosted.implementing) {
                                            InterfaceInfo implemented = documents.get(implementingName).interfaceInfo;
                                            rtb.table(RuntimeTable.TableType.Interface, dependency.name + "." + implemented.name);
                                            for (String method : implemented.methods) {
                                                HostedInfo runtimeValues = dependency.hosted;
                                                int size = runtimeValues.methods.get(method).size;
                                                int addr = runtimeValues.methods.get(method).addr + hostedValuesMap.get(dependency).methodStartAddr;
                                                rtb.entry(RuntimeTable.EntryType.Method, implemented.name + "." + method, size, addr);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    RuntimeTable rt = rtb.table();
                    hostInfo.runtimeTable = rt;

                    // Write runtime table to a free page
                    int[] rtValues = rt.values();
                    int rtPageCount = 1 + (rtValues.length / PAGE_SIZE);
                    int[] runtimePages = allocate(rtPageCount);
                    if (runtimePages.length > 1) throw new RuntimeException("runtime table size not supported");
                    int runtimePage = runtimePages[0];
                    for (int i = 0; i < rtValues.length; i++) {
                        byte[] rtBytes = machine.VirtualMachine.intToBytes(rtValues[i]);
                        System.arraycopy(rtBytes, 0, m.pages[runtimePage], (i * 4), 4);
                    }

                    //  MAKE TEMPLATE

                    // make a template page using the pageMap, runtime table, and all imported class methods
                    ArrayList<Integer> templatePage = new ArrayList<>();
                    templatePage.add(runtimePage);
                    templatePage.add(allocate()); // SystemIn
                    templatePage.add(allocate()); // SystemOut
                    templatePage.add(allocate()); // Root
                    templatePage.add(allocate()); // Allocator
                    for (Document document : dependencies) {
                        switch (document.type) {
                            case Interface, Protocol -> {}
                            case Hosted -> {
                                HostedInfo thisHostedInfo = document.hosted;
                                for (int cPage : thisHostedInfo.constPages) {
                                    templatePage.add(cPage);
                                }
                                for (int mPage : thisHostedInfo.methodPages) {
                                    templatePage.add(mPage);
                                }
                            }
                            case Host -> {
                                for (int cPage : hostInfo.constPages) {
                                    templatePage.add(cPage);
                                }

                                for (int[] pPages : protocolPages) {
                                    for (int pPage : pPages) {
                                        templatePage.add(pPage);
                                    }
                                }
                                for (int mPages : hostInfo.methodPages) {
                                    templatePage.add(mPages);
                                }
                            }
                        }

                    }

                    byte[] templatePageAsBytes = new byte[templatePage.size()];
                    for (int i = 0; i < templatePageAsBytes.length; i++) {
                        templatePageAsBytes[i] = (byte) (int) templatePage.get(i);
                    }

                    hostInfo.pagesTemplate = templatePageAsBytes;
                    // assume the first method
                    hostInfo.initAddr = hostedValuesMap.get(iDoc).methodStartAddr;
                    hostInfo.adminTable = documentTableEntries.get(iHostAdmin).get(iHostAdmin) * 4;
                    this.hostInfo.put(name, hostInfo);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    int initHost(String documentName) {
        Document document = documents.get(documentName);
        if (document.type != Types.Document.Host) throw new RuntimeException();

        // get the template
        HostInfo info = document.host;

        int objectId = currentInstance++;

        // Copy the Host's template page map to its instance page map
        int pageMap = allocate();
        System.arraycopy(info.pagesTemplate, 0, m.pages[pageMap], 0, info.pagesTemplate.length);

        int taskMetaSize = 20;
        // Use free space at the end of the root object page to set up thread and frame
        int methodSize = taskMetaSize + info.methods.get("main").size + SECONDARY_SIZE;
        int frame = ROOT_OBJECT_PAGE * PAGE_SIZE - methodSize + taskMetaSize;
        int adminTask = frame + methodSize - SECONDARY_SIZE;
        int object = ROOT_OBJECT_PAGE * PAGE_SIZE;
        int adminObject = PAGE_SIZE * ALLOCATOR_PAGE;

        Processor.Snapshot s = new Processor.Snapshot();
        s.instance = objectId;
        s.pageMap = pageMap;

        s.interrupted = false;
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

        Administrator.Host host = new Host();
        host.address = objectId;
        host.template = info;
        host.rootAddr = ROOT_OBJECT_PAGE * PAGE_SIZE;
        host.pageMap = pageMap;
        host.memberOut = new IO.MemberOut(pageMap, MEMBER_OUT_PAGE * PAGE_SIZE, m);
        host.memberIn = new IO.MemberIn(pageMap, MEMBER_IN_PAGE * PAGE_SIZE, m);
        hosts.put(objectId, host);


        addQuota(host, pageMap, s);
        schedule();

        return objectId;
    }

    public void notify(int instance, int interruptValue) {
        enum AdminMethod {
            Unknown,
            ConnectRequest,
            ConnectResponse,
            AllocatePage,
            Exit,
            Send
        }
        Administrator.Host info = hosts.get(instance);
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
                int page1 = memberIn.readInt();
                int page2 = memberIn.readInt();
                int[] pages = new int[2];
                pages[0] = page1;
                pages[1] = page2;

                Administrator.Host receiver = hosts.get(receiverInstance);
                receiver.memberOut.write(2);
                receiver.memberOut.write(instance);
                receiver.memberOut.write(protocol);
                receiver.memberOut.commit();
                receiver.awaitingResponse = true;
                receiver.clientInstance = instance;
                receiver.pages = pages;
            }
            case ConnectResponse -> {
                if (!info.awaitingResponse) throw new RuntimeException("not awaiting a connection response");
                int pageCount = memberIn.readInt();
                int[] pages = new int[pageCount];
                for (int i = 0; i < pageCount; i++) {
                    pages[i] = memberIn.readInt();
                }
                byte[] clientPageMap = m.pages[hosts.get(info.clientInstance).pageMap];
                byte[] serverPageMap = m.pages[hosts.get(info.address).pageMap];
                for (int i = 0; i < info.pages.length; i++) {
                    serverPageMap[pages[i] / VirtualMachine.PAGE_SIZE] = clientPageMap[info.pages[i] / VirtualMachine.PAGE_SIZE];
                }
            }
            case AllocatePage -> {
                int localPage = memberIn.readInt();
                allocate(instance, localPage);
            }
            case Exit -> {
                m.processors.getFirst().snapshot(scheduled.snapshot);
                scheduled.status = LogicianQuota.Status.Complete;
                schedule();
            }
            case Send -> {
                int targetInstance = memberIn.readInt();
                schedule(targetInstance);
            }
            default -> throw new RuntimeException("Bad interrupt type");
        }
        memberIn.update();
    }

    Inspector inspect(int hostId) {
        Host host = hosts.get(hostId);
        Inspector inspector = new Inspector();
        inspector.pageMap = host.pageMap;
        inspector.machine = m;
        inspector.host = host;
        inspector.admin = this;
        inspector.snapshot();

        return inspector;
    }

    // Page allocator

    // Sets a free page to the local page in the objects page map
    void allocate(int objectId, int localPage) {
        int pageMap = hosts.get(objectId).pageMap;
        byte[] page = m.pages[pageMap];
        int next = allocate();
        page[localPage] = (byte) next;
    }

    // Used by the system to make runtime/method pages
    int allocate() {
        return freePage++;
    }

    int[] allocate(int count) {
        int[] pages = new int[count];
        for (int i = 0; i < count; i++) {
            pages[i] = freePage++;
        }
        return pages;
    }

    // Scheduling

    void addQuota(Host host, int pageMap, Processor.Snapshot snapshot) {
        LogicianQuota t = new LogicianQuota();
        t.host = host;
        t.pageMap = pageMap;
        t.status = LogicianQuota.Status.Incomplete;
        t.snapshot = snapshot;
        quotas.add(t);
        host.quotas[0] = t;
    }

    Optional<LogicianQuota> find(int instance) {
        for (LogicianQuota quote : quotas) {
            if (quote.host.address == instance) {
                return Optional.of(quote);
            }
        }
        return Optional.empty();
    }

    void schedule() {
        for (LogicianQuota t : quotas) {
            if (t.status != LogicianQuota.Status.Complete) {
                schedule(t);
                return;
            }
        }
    }

    void schedule(int instance) {
        for (LogicianQuota t : quotas) {
            if (t.status != LogicianQuota.Status.Complete && instance == t.host.address) {
                schedule(t);
                return;
            }
        }
    }

    void schedule(LogicianQuota t) {
        Processor processor = m.processors.getFirst();
        if (this.scheduled != null) {
            processor.snapshot(scheduled.snapshot);
        }
        processor.load(t.snapshot);
        scheduled = t;
    }

    record FieldRuntimeValues (int addr          ) {}
    record DataRuntimeValue   (String name, int size, int addr) {}
    record ConstRuntimeValues (int size, int addr) {}
    record Symbol             (Types.Symbol type, String identifier) {}
    static class MethodRuntimeValues {
        String name;
        int size;
        int addr;
        int endAddr;
        HashMap<String, DataRuntimeValue> data = new HashMap<>();
    }
    static class HostedInfo {
        String name;
        int[] constPages;
        int[] methodPages;
        int size;
        ArrayList<String> dependencies = new ArrayList<>();
        ArrayList<Symbol> symbols = new ArrayList<>();
        ArrayList<String> implementing = new ArrayList<>();
        HashMap<String, MethodRuntimeValues> methods = new HashMap<>();
        HashMap<String, FieldRuntimeValues> fields = new HashMap<>();
        HashMap<String, ConstRuntimeValues> constants = new HashMap<>();
    }

    static class HostedValues {
        String name;
        int constantsStartAddr;
        HashMap<String, Integer> protocolsAddresses = new HashMap<>();
        int methodStartAddr;
        int table;
        // currently used by inspector, rework it?
        int[] methodPages;
        HashMap<String, MethodRuntimeValues> methods;
    }

    static class HostInfo {
        String name;
        int[] constPages;
        int[][] protocolPages;
        int[] methodPages;
        int size;
        ArrayList<Symbol> symbols = new ArrayList<>();
        ArrayList<InterfaceInfo> implementing = new ArrayList<>();
        HashMap<String, MethodRuntimeValues> methods = new HashMap<>();
        HashMap<String, FieldRuntimeValues> fields = new HashMap<>();
        HashMap<String, ConstRuntimeValues> constants = new HashMap<>();

        HashMap<String, HostedValues> hostedValues = new HashMap<>();
        HostedValues administrator;

        byte[] pagesTemplate;
        int initAddr;
        int adminTable;
        int protocolsTable;

        RuntimeTable runtimeTable;
    }

    static class InterfaceInfo {
        String name;
        ArrayList<String> methods = new ArrayList<>();
        Document doc;
    }

    static class ProtocolInfo {
        String name;
        int[] pages;
        LinkedHashMap<String, ProtocolMethodInfo> methods = new LinkedHashMap<>();
    }

    static class ProtocolMethodInfo {
        int id;
        String name;
        int addr;
        Port[] ports;
    }

    static class Port {
        int size;
        int permissions;
    }

    static class Host {
        int address;
        int pageMap;
        int rootAddr;
        IO.MemberOut memberOut;
        IO.MemberIn memberIn;
        HostInfo template;

        boolean awaitingResponse = false;
        int clientInstance;
        int[] pages;
        LogicianQuota[] quotas = new LogicianQuota[8];
    }

    // maybe have an index and a type?
    static class Document {
        String name;
        Types.Document type;
        HostInfo host;
        HostedInfo hosted;
        InterfaceInfo interfaceInfo;
        ProtocolInfo protocolInfo;
    }

    static class LogicianQuota {
        enum Status {
            Incomplete, Complete
        }
        Host host;
        int pageMap;
        Status status;
        Processor.Snapshot snapshot;
    }

}
