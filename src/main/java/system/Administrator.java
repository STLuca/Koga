package system;

import core.Instruction;
import machine.Notifiable;
import machine.Processor;
import machine.VirtualMachine;

import java.util.*;

import static machine.VirtualMachine.PAGE_SIZE;
import static machine.VirtualMachine.intToBytes;

public class Administrator implements Notifiable {

    enum Type {
        Host,
        Hosted,
        Interface,
        Protocol
    }

    public static class Symbol {

        public Type type;
        public String identifier;

        public enum Type {
            CLASS,
            INTERFACE,
            FIELD,
            METHOD,
            CONST,
            SYSTEM,

            PROTOCOLS,
            PROTOCOL
        }
    }

    final static int MEMBER_OUT_PAGE   = 1;
    final static int MEMBER_IN_PAGE    = 2;
    final static int ROOT_OBJECT_PAGE  = 3;
    final static int ALLOCATOR_PAGE    = 4;
    final static int HOSTED_START      = 5;

    final static int SECONDARY_SIZE = 200;
    final static int INSTRUCTION_SIZE = 18;

    VirtualMachine m;

    // source
    HashMap<String, core.Document> sourceDocuments = new HashMap<>();

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

    // Inspecting
    HashMap<String, InspectInfo> inspectInfo = new HashMap<>();

    Administrator() {
        this.m = new VirtualMachine(1, this);
    }

    public static Administrator boot() {
        return new Administrator();
    }

    // Compiler

    byte[] compile(core.Document doc) {
        int instructionBytesSize = 0;
        for (core.Document.Method m : doc.methods) {
            instructionBytesSize += m.instructions.length * INSTRUCTION_SIZE;
        }
        int byteIdx = 0;
        byte[] bytes = new byte[instructionBytesSize];

        for (core.Document.Method m : doc.methods) {
            int[] addresses = new int[m.instructions.length];
            int i = 0;
            int curr = 0;
            for (Instruction ignored : m.instructions) {
                addresses[i] = curr;
                i++;
                curr += INSTRUCTION_SIZE;
                // 1 byte for type, subType, inputType, src1Size, src2Size, src3Size
                // 4 bytes for src1, src2, src3
            }

            curr = 0;
            for (Instruction in : m.instructions) {
                bytes[byteIdx++] = (byte) in.type.ordinal();
                int inType = switch (in.type) {
                    case Integer -> in.lType.ordinal();
                    case Jump -> in.jType.ordinal();
                    case ConditionalBranch -> in.bType.ordinal();
                    case Class -> in.cmType.ordinal();
                    case Logician -> in.lgType.ordinal();
                    case Memory -> in.mType.ordinal();
                    case Debug -> in.dType.ordinal();
                    case Math -> in.mathType.ordinal();
                    case Float -> in.fType.ordinal();
                    case Atomic -> in.aType.ordinal();
                    case Vector -> in.vType.ordinal();
                };
                bytes[byteIdx++] = (byte) inType;
                bytes[byteIdx++] = (byte) in.inputType.ordinal();

                int dest;
                int src1 = in.src2;
                switch (in.type) {
                    case ConditionalBranch -> {
                        dest = addresses[in.src1] - addresses[curr];
                    }
                    case Jump -> {
                        switch (in.inputType) {
                            case I -> {
                                dest = addresses[in.src1] - addresses[curr];
                                src1 = addresses[in.src1] - addresses[curr];
                            }
                            default -> {
                                dest = in.src1;
                            }
                        }
                    }
                    default -> dest = in.src1;
                }

                bytes[byteIdx++] = (byte) in.src1Size;
                for (Byte b : intToBytes(dest)) {
                    bytes[byteIdx++] = b;
                }

                bytes[byteIdx++] = (byte) in.src2Size;
                for (Byte b : intToBytes(src1)) {
                    bytes[byteIdx++] = b;
                }

                bytes[byteIdx++] = (byte) in.src3Size;
                for (Byte b : intToBytes(in.src3)) {
                    bytes[byteIdx++] = b;
                }

                curr++;
            }
        }
        return bytes;
    }

    // adding a class
    void integrate(core.Document doc) {
        if (!sourceDocuments.containsKey(doc.name)) {
            sourceDocuments.put(doc.name, doc);
        }
        if (documents.containsKey(doc.name)) return;

        Document iDoc = new Document();
        documents.put(doc.name, iDoc);
        iDoc.name = doc.name;

        switch (doc.type) {
            case Interface -> {
                InterfaceInfo interfaceInfo = new InterfaceInfo();
                interfaceInfo.name = doc.name;
                for (int i = 0; i < doc.methods.length; i++) {
                    interfaceInfo.methods.add(doc.methods[i].name);
                }
                this.interfaceInfo.put(doc.name, interfaceInfo);

                interfaceInfo.doc = iDoc;
                iDoc.type = Type.Interface;
                iDoc.interfaceInfo = interfaceInfo;
            }
            case Protocol -> {
                ProtocolInfo pInfo = new ProtocolInfo();
                pInfo.name = doc.name;
                int currentAddr = 4;
                for (core.Document.ProtocolMethod pm : doc.protocolMethods) {
                    ProtocolMethodInfo pmInfo = new ProtocolMethodInfo();
                    pmInfo.name = pm.name;
                    pmInfo.id = nextProtocolMethodId++;
                    pmInfo.addr = currentAddr;
                    currentAddr += 12 + pm.ports.length * 8;
                    pInfo.methods.put(pm.name, pmInfo);
                }


                //  methodCount
                //  methods { id, symbol, portCount }
                //      ports { size, permissions }
                int byteCount = 0;
                for (core.Document.ProtocolMethod pm : doc.protocolMethods) {
                    byteCount += 16 + pm.ports.length * 8;
                }
                byte[] bytes = new byte[byteCount];

                int byteIndex = 0;
                int currentSymbol = 0;
                for (Byte b : intToBytes(doc.protocolMethods.length)) {
                    bytes[byteIndex++] = b;
                }
                for (core.Document.ProtocolMethod pm : doc.protocolMethods) {
                    for (Byte b : intToBytes(pInfo.methods.get(pm.name).id)) {
                        bytes[byteIndex++] = b;
                    }
                    for (Byte b : intToBytes(currentSymbol++)) {
                        bytes[byteIndex++] = b;
                    }
                    for (Byte b : intToBytes(pm.ports.length)) {
                        bytes[byteIndex++] = b;
                    }
                    for (core.Document.Port port : pm.ports) {
                        for (Byte b : intToBytes(port.size())) {
                            bytes[byteIndex++] = b;
                        }
                        for (Byte b : intToBytes(port.permission())) {
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
                protocolInfo.put(doc.name, pInfo);

                iDoc.type = Type.Protocol;
                iDoc.protocolInfo = pInfo;
            }
            case Hosted -> {
                // Add the methods
                byte[] bytes = compile(doc);
                int pageCount = 1 + (bytes.length / PAGE_SIZE);
                int[] methodPages = allocate(pageCount);
                int x = 0;
                for (int i = 0; i < pageCount; i++) {
                    for (int ii = 0; ii < VirtualMachine.PAGE_SIZE && x < bytes.length; ii++) {
                        m.pages[methodPages[i]][ii] = bytes[x];
                        x++;
                    }
                }

                // Adds the constants
                int constsSize = 0;
                for (int i = 0; i < doc.consts.length; i++) {
                    constsSize += doc.consts[i].value.length;
                }

                int[] constMap = new int[doc.consts.length];
                int byteIdx = 0;
                byte[] constBytes = new byte[constsSize];
                for (int i = 0; i < doc.consts.length; i++) {
                    constMap[i] = byteIdx;
                    for (byte b : doc.consts[i].value) {
                        constBytes[byteIdx++] = b;
                    }
                }

                int constPageCount = constsSize == 0 ? 0 : 1 + (constsSize / PAGE_SIZE);
                int[] constPages = allocate(constPageCount);
                int cbi = 0; // const byte index
                for (int i = 0; i < constPageCount; i++) {
                    for (int ii = 0; ii < VirtualMachine.PAGE_SIZE && cbi < constBytes.length; ii++) {
                        m.pages[constPages[i]][ii] = constBytes[cbi];
                        cbi++;
                    }
                }

                // Set up the class pages

                // Create the runtime information
                HostedInfo hostedInfo = new HostedInfo();
                hostedInfo.document = doc;
                hostedInfo.size = doc.size;
                hostedInfo.methodPages = methodPages;
                hostedInfo.constPages = constPages;
                for (core.Document.Data data : doc.data) {
                    hostedInfo.fields.put(data.name(), new FieldRuntimeValues(data.start()));
                }

                int startAddr = 0;
                for (core.Document.Method method : doc.methods) {
                    int methodSize = method.size;
                    int endAddr = startAddr + method.instructions.length * INSTRUCTION_SIZE;
                    MethodRuntimeValues mrv = new MethodRuntimeValues(methodSize, startAddr, endAddr);
                    hostedInfo.methods.put(method.name, mrv);
                    startAddr = endAddr;
                }

                // Const table
                for (int i = 0; i < doc.consts.length; i++) {
                    core.Document.Const constant = doc.consts[i];
                    hostedInfo.constants.put(constant.name, new ConstRuntimeValues(constant.value.length, constMap[i]));
                }
                hostedInfo.dependencies.addAll(Arrays.asList(doc.dependencies));
                for (core.Document.Symbol dSymbol : doc.symbols) {
                    Symbol symbol = new Symbol();
                    symbol.identifier = dSymbol.identifier;
                    symbol.type = Symbol.Type.values()[dSymbol.type.ordinal()];
                    hostedInfo.symbols.add(symbol);
                }
                hostedInfo.implementing.addAll(Arrays.asList(doc.implementing));

                this.hostedInfo.put(doc.name, hostedInfo);

                iDoc.type = Type.Hosted;
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


                // COMPILE

                // Add the methods
                byte[] bytes = compile(doc);
                int pageCount = 1 + (bytes.length / PAGE_SIZE);
                int[] methodPages = allocate(pageCount);
                int x = 0;
                for (int i = 0; i < pageCount; i++) {
                    for (int ii = 0; ii < VirtualMachine.PAGE_SIZE && x < bytes.length; ii++) {
                        m.pages[methodPages[i]][ii] = bytes[x];
                        x++;
                    }
                }

                // Adds the constants
                int constsSize = 0;
                for (int i = 0; i < doc.consts.length; i++) {
                    constsSize += doc.consts[i].value.length;
                }

                int[] constMap = new int[doc.consts.length];
                int byteIdx = 0;
                byte[] constBytes = new byte[constsSize];
                for (int i = 0; i < doc.consts.length; i++) {
                    constMap[i] = byteIdx;
                    for (byte b : doc.consts[i].value) {
                        constBytes[byteIdx++] = b;
                    }
                }

                int constPageCount = constsSize == 0 ? 0 : 1 + (constsSize / PAGE_SIZE);
                int[] constPages = allocate(constPageCount);
                int cbi = 0; // const byte index
                for (int i = 0; i < constPageCount; i++) {
                    for (int ii = 0; ii < VirtualMachine.PAGE_SIZE && cbi < constBytes.length; ii++) {
                        m.pages[constPages[i]][ii] = constBytes[cbi];
                        cbi++;
                    }
                }

                // Create the runtime information
                HostInfo hostInfo = new HostInfo();
                hostInfo.name = doc.name;
                hostInfo.size = doc.size;
                hostInfo.methodPages = methodPages;
                hostInfo.constPages = constPages;
                for (core.Document.Data data : doc.data) {
                    hostInfo.fields.put(data.name(), new FieldRuntimeValues(data.start()));
                }
                int startAddr = 0;
                for (core.Document.Method method : doc.methods) {
                    int methodSize = method.size;
                    int endAddr = startAddr + method.instructions.length * INSTRUCTION_SIZE;
                    MethodRuntimeValues mrv = new MethodRuntimeValues(methodSize, startAddr, endAddr);
                    hostInfo.methods.put(method.name, mrv);
                    startAddr = endAddr;
                }
                for (int i = 0; i < doc.consts.length; i++) {
                    core.Document.Const constant = doc.consts[i];
                    hostInfo.constants.put(constant.name, new ConstRuntimeValues(constant.value.length, constMap[i]));
                }

                for (core.Document.Symbol dSymbol : doc.symbols) {
                    Symbol symbol = new Symbol();
                    symbol.identifier = dSymbol.identifier;
                    symbol.type = Symbol.Type.values()[dSymbol.type.ordinal()];
                    hostInfo.symbols.add(symbol);
                }

                iDoc.type = Type.Host;
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
                for (String dependency : doc.dependencies) {
                    Document iDocument = documents.get(dependency);
                    toResolve.add(iDocument);
                }

                while (!toResolve.isEmpty()) {
                    Document resolve = toResolve.pop();
                    if (dependencies.contains(resolve)) continue;
                    dependencies.add(resolve);
                    if (resolve.name.equals(doc.administrator)) {
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
                    if (protocolDependency.type == Type.Protocol) {
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
                            hv.constantsStartAddr = currentPage * VirtualMachine.PAGE_SIZE;
                            currentPage += hostedInfo.constPages.length;
                            hv.methodStartAddr = currentPage * VirtualMachine.PAGE_SIZE;
                            currentPage += hostedInfo.methodPages.length;
                            hostedValuesMap.put(iDocument, hv);
                            hv.methodPages = hostedInfo.methodPages;
                            hv.methods = hostedInfo.methods;
                            hv.document = hostedInfo.document;
                            hostInfo.hostedValues.put(dependency.name, hv);
                        }
                        case Host -> {
                            HostedValues hv = new HostedValues();
                            hv.constantsStartAddr = currentPage * VirtualMachine.PAGE_SIZE;
                            currentPage += constPages.length;
                            for (ProtocolInfo protocolDependency : protocolDependencies) {
                                hv.protocolsAddresses.put(protocolDependency.name, currentPage * VirtualMachine.PAGE_SIZE);
                                currentPage += protocolDependency.pages.length;
                            }
                            hv.methodStartAddr = currentPage * VirtualMachine.PAGE_SIZE;
                            currentPage += methodPages.length;
                            hostedValuesMap.put(iDocument, hv);
                            hv.methodPages = hostInfo.methodPages;
                            hv.methods = hostInfo.methods;
                            hv.document = doc;
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
                            if (dependency.type == Type.Host) {
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
                                                int protocolAddr = hostInfo.hostedValues.get(doc.name).protocolsAddresses.get(pInfo.name);
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
                            for (int cPage : constPages) {
                                templatePage.add(cPage);
                            }

                            for (int[] pPages : protocolPages) {
                                for (int pPage : pPages) {
                                    templatePage.add(pPage);
                                }
                            }
                            for (int mPages : methodPages) {
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
                this.hostInfo.put(doc.name, hostInfo);
            }
        }
    }

    int initHost(String documentName) {
        Document document = documents.get(documentName);
        if (document.type != Type.Host) throw new RuntimeException();

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
        record Entry(int start, int end) {}
        Host host = hosts.get(hostId);
        HostInfo hostInfo = host.template;

        Inspector inspector = new Inspector();
        HashMap<Entry, core.Document.Method> methodByInstruction = new HashMap<>();
        inspector.pageMap = host.pageMap;
        inspector.machine = m;

        if (!inspectInfo.containsKey(hostInfo.name)) {
            InspectInfo inspectInfo = new InspectInfo();
            inspectInfo.host = host;
        }

        // Methods
        // Use class runtime values to create methods
        Inspector.Host iHost = new Inspector.Host();
        iHost.runtimeTable = hostInfo.runtimeTable.toString();
        inspector.host = iHost;

        byte[] pageMap = hostInfo.pagesTemplate;
        int i = hostInfo.hostedValues.get(hostInfo.name).methodStartAddr / PAGE_SIZE; // skip runtime table and member components
        loop: while (i < pageMap.length) {
            byte page = pageMap[i];
            for (HostedValues hostedInfo : hostInfo.hostedValues.values()) {
                if ((byte) hostedInfo.methodPages[0] != page) continue;
                for (String methodName : hostedInfo.methods.keySet()) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Data:\n");
                    core.Document methodDocument = hostedInfo.document;
                    core.Document.Method method = Arrays.stream(methodDocument.methods).filter(m -> m.name.equals(methodName)).findFirst().orElseThrow();
                    TreeSet<core.Document.Data> newOrderedData = new TreeSet<>(Comparator.comparing(core.Document.Data::start));
                    for (core.Document.Data d : method.data) {
                        if (d.name().contains(".")) {
                            newOrderedData.add(d);
                        }
                    }
                    for (core.Document.Data data : newOrderedData) {
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
                    MethodRuntimeValues mvs = hostedInfo.methods.get(methodName);
                    int methodAddr = (PAGE_SIZE * (i)) + mvs.addr();
                    int methodEndAddr = (PAGE_SIZE * (i)) + mvs.endAddr();
                    methodByInstruction.put(new Entry(methodAddr, methodEndAddr), method);
                    for (int curr = methodAddr; curr < methodEndAddr; curr += 18) {
                        if (curr < 10) sb.append(" ");
                        if (curr < 100) sb.append(" ");
                        sb.append(curr).append(": ");

                        byte typeIndex = m.loadByte(host.pageMap, curr);
                        byte subType   = m.loadByte(host.pageMap, curr + 1);
                        byte inType    = m.loadByte(host.pageMap, curr + 2);
                        byte destSize  = m.loadByte(host.pageMap, curr + 3);
                        byte dest      = m.loadByte(host.pageMap, curr + 4);
                        byte src1Size  = m.loadByte(host.pageMap, curr + 8);
                        byte src1      = m.loadByte(host.pageMap, curr + 9);
                        byte src2Size  = m.loadByte(host.pageMap, curr + 13);
                        byte src2      = m.loadByte(host.pageMap, curr + 14);

                        Instruction.Type type = Instruction.Type.values()[typeIndex];
                        sb.append(type).append(" ");
                        switch (type) {
                            case Integer -> sb.append(Instruction.IntegerType.values()[subType]);
                            case Jump -> sb.append(Instruction.BranchType.values()[subType]);
                            case ConditionalBranch -> sb.append(Instruction.ConditionalBranchType.values()[subType]);
                            case Class -> sb.append(Instruction.ClassType.values()[subType]);
                            case Logician -> sb.append(Instruction.LogicianType.values()[subType]);
                            case Memory -> sb.append(Instruction.MemoryType.values()[subType]);
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
                    }

                    iHost.methods.put(methodDocument.name + "." + methodName, sb.toString());
                }

                i+=hostedInfo.methodPages.length;
                continue loop;
            }
        }

        for (LogicianQuota quota : quotas) {
            if (quota.host.address != hostId) continue;
            Processor.Snapshot snapshot = quota.snapshot;
            if (quota == scheduled) {
                m.processors.getFirst().snapshot(snapshot);
            }
            Inspector.Task t = new Inspector.Task();

            t.task = snapshot.task;
            t.object = snapshot.object;
            t.table = snapshot.table;
            t.instruction = snapshot.instruction;
            t.altTask = snapshot.altTask;
            t.altObject = snapshot.altObject;
            t.altTable = snapshot.altTable;
            t.altInstruction = snapshot.altInstruction;

            // TODO: currently only works once method has moved one instruction and admin method has moved one instruction
            Entry entry = methodByInstruction.keySet().stream()
                    .filter(e -> t.instruction > e.start && t.instruction <= e.end)
                    .findFirst()
                    .orElseThrow();
            core.Document.Method method = methodByInstruction.get(entry);
            for (core.Document.Data data : method.data) {
                if (data.name().contains(".") && data.size() <= 4) {
                    String[] split = data.name().split("\\.");
                    t.data.putIfAbsent(split[0], new HashMap<>());
                    t.data.get(split[0]).put(split[1], m.loadInt(host.pageMap, t.task + data.start(), data.size()));
                }
            }

            entry = methodByInstruction.keySet().stream()
                    .filter(e -> t.altInstruction > e.start && t.altInstruction <= e.end)
                    .findFirst()
                    .orElseThrow();

            method = methodByInstruction.get(entry);
            for (core.Document.Data data : method.data) {
                if (data.name().contains(".") && data.size() <= 4) {
                    String[] split = data.name().split("\\.");
                    t.altData.putIfAbsent(split[0], new HashMap<>());
                    t.altData.get(split[0]).put(split[1], m.loadInt(host.pageMap, t.altTask + data.start(), data.size()));
                }
            }

            inspector.tasks.add(t);
        }

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


    record FieldRuntimeValues (int addr                       ) {}
    record ConstRuntimeValues (int size, int addr             ) {}
    record MethodRuntimeValues(int size, int addr, int endAddr) {}
    static class HostedInfo {
        core.Document document;
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
        int constantsStartAddr;
        HashMap<String, Integer> protocolsAddresses = new HashMap<>();
        int methodStartAddr;
        int table;
        // currently used by inspector, rework it?
        int[] methodPages;
        HashMap<String, MethodRuntimeValues> methods;
        core.Document document;
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
        HashMap<String, ProtocolMethodInfo> methods = new HashMap<>();
    }

    static class ProtocolMethodInfo {
        int id;
        String name;
        int addr;
        // Port ports
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
        Type type;
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

    // INSPECTING

    static class InspectInfo {
        Host host;
        HashMap<String, MethodInspect> methods = new HashMap<>();
        ArrayList<TaskInspect> tasks = new ArrayList<>();
    }

    static class TaskInspect {
        HashMap<String, Map<String, Integer>> data = new HashMap<>();
        HashMap<String, Map<String, Integer>> altData = new HashMap<>();
        int task;
        int object;
        int instruction;
        int table;
        int altTask;
        int altObject;
        int altInstruction;
        int altTable;
    }

    static class MethodInspect {
        ArrayList<MethodDataInspect> data = new ArrayList<>();

    }

    static class MethodDataInspect {
        String name;
        int position;
        int size;
    }
}
