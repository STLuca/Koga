package system;

import machine.VirtualMachine;

/*
    status: Starting, running...
    space
        page size
        page count
    logicians
        ...
    connections[]
        host
        status
    outbox
        connectionIndex
        startByte
        count
    inbox
        connectionIndex
        startByte
        count
    log
 */
public class SystemProtocol {

    static final int ID_OFFSET = 0;
    static final int STATUS_OFFSET = ID_OFFSET + Id.SIZE;
    static final int SPACE_OFFSET = STATUS_OFFSET + Status.SIZE;
    static final int LOGICIANS_OFFSET = SPACE_OFFSET + Space.SIZE;
    static final int CONNECTIONS_OFFSET = LOGICIANS_OFFSET + Logicians.SIZE;
    static final int OUTBOX_OFFSET = CONNECTIONS_OFFSET + Connections.SIZE;
    static final int INBOX_OFFSET = OUTBOX_OFFSET + Messages.SIZE;

    class Id {

        static final int SIZE = 4;
        static int ID_FIELD_OFFSET;

        void set(int id) {
            machine.store(pageMap, root + ID_OFFSET + ID_FIELD_OFFSET, id);
        }

        int get() {
            return machine.loadInt(pageMap, root + ID_OFFSET + ID_FIELD_OFFSET);
        }

    }

    class Status {

        static final int SIZE = 4;
        static final int TYPE_OFFSET = 0;

        enum Type {
            Starting,
            Running,
            Exited
        }

        void type(Type type) {
            int typeOrdinal = type.ordinal();
            machine.store(pageMap, root + STATUS_OFFSET + TYPE_OFFSET, typeOrdinal);
        }

        Type type() {
            int typeOrdinal = machine.loadInt(pageMap, root + STATUS_OFFSET + TYPE_OFFSET);
            return Type.values()[typeOrdinal];
        }

    }

    class Space {

        static final int SIZE = 12;
        static final int PAGE_SIZE_OFFSET = 0;
        static final int PAGE_COUNT_OFFSET = 4;
        static final int PAGE_DESIRED_COUNT_OFFSET = 8;

        void pageSize(int pageSize) {
            machine.store(pageMap, root + SPACE_OFFSET + PAGE_SIZE_OFFSET, pageSize);
        }

        void count(int count) {
            machine.store(pageMap, root + SPACE_OFFSET + PAGE_COUNT_OFFSET, count);
        }

        int count() {
            return machine.loadInt(pageMap, root + SPACE_OFFSET + PAGE_COUNT_OFFSET);
        }

        void desiredCount(int count) {
            machine.store(pageMap, root + SPACE_OFFSET + PAGE_DESIRED_COUNT_OFFSET, count);
        }

        int desiredCount() {
            return machine.loadInt(pageMap, root + SPACE_OFFSET + PAGE_DESIRED_COUNT_OFFSET);
        }

    }

    class Logicians {

        static final int SIZE = 36;
        static final int COUNT_OFFSET = 0;
        static final int LOGICIANS_FIELD_OFFSET = 4;

        int count() {
            return machine.loadInt(pageMap, root + LOGICIANS_OFFSET + COUNT_OFFSET);
        }

        Logician logician(int index) {
            int logiciansStart = LOGICIANS_OFFSET + LOGICIANS_FIELD_OFFSET;

            Logician logician = new Logician();
            logician.offset = logiciansStart + index * Logician.SIZE;
            return logician;
        }

    }

    class Logician {

        enum Status {
            Running,
            Waiting
        }

        static final int SIZE = 8;
        static final int STATUS_OFFSET = 0;

        int offset;

        Status status() {
            int statusOrdinal = machine.loadInt(pageMap, root + offset + STATUS_OFFSET);
            return Status.values()[statusOrdinal];
        }

    }

    class Connections {

        static final int SIZE = 36;
        static final int COUNT_OFFSET = 0;
        static final int CONNECTIONS_FIELD_OFFSET = 4;

        int count() {
            return machine.loadInt(pageMap, root + CONNECTIONS_OFFSET + COUNT_OFFSET);
        }

        Connection connection(int index) {
            int connectionsStart = CONNECTIONS_OFFSET + CONNECTIONS_FIELD_OFFSET;

            Connection connection = new Connection();
            connection.offset = connectionsStart + index * Connection.SIZE;
            return connection;
        }

    }

    class Connection {

        static final int SIZE = 16;
        static final int HOST_OFFSET = 0;
        static final int PROTOCOL_OFFSET = 4;
        static final int PAGE_ONE_OFFSET = 8;
        static final int PAGE_TWO_OFFSET = 12;

        int offset;

        int host() {
            return machine.loadInt(pageMap, root + offset + HOST_OFFSET);
        }

        void host(int host) {
            machine.store(pageMap, root + offset + HOST_OFFSET, host);
        }

        int protocol() {
            return machine.loadInt(pageMap, root + offset + PROTOCOL_OFFSET);
        }

        void protocol(int protocol) {
            machine.store(pageMap, root + offset + PROTOCOL_OFFSET, protocol);
        }

        int pageOne() {
            return machine.loadInt(pageMap, root + offset + PAGE_ONE_OFFSET);
        }

        void pageOne(int page) {
            machine.store(pageMap, root + offset + PAGE_ONE_OFFSET, page);
        }

        int pageTwo() {
            return machine.loadInt(pageMap, root + offset + PAGE_TWO_OFFSET);
        }

        void pageTwo(int page) {
            machine.store(pageMap, root + offset + PAGE_TWO_OFFSET, page);
        }

    }

    class Messages {

        static final int SIZE = 36;
        static final int COUNT_OFFSET = 0;
        static final int VALUES_OFFSET = 4;

        int offset;

        int count() {
            return machine.loadInt(pageMap, root + offset + COUNT_OFFSET);
        }

        Message message(int index) {
            int messageStart = offset + VALUES_OFFSET;

            Message message = new Message();
            message.offset = messageStart + index * Message.SIZE;
            return message;
        }

    }

    class Message {

        static final int SIZE = 12;
        static final int CONNECTION_INDEX_OFFSET = 0;
        static final int START_OFFSET = 4;
        static final int COUNT_OFFSET = 8;

        int offset;

        int connectionIndex() {
            return machine.loadInt(pageMap, root + offset + CONNECTION_INDEX_OFFSET);
        }

        void connectionIndex(int index) {
            machine.store(pageMap, root + offset + CONNECTION_INDEX_OFFSET, index);
        }

        int start() {
            return machine.loadInt(pageMap, root + offset + START_OFFSET);
        }

        int count() {
            return machine.loadInt(pageMap, root + offset + COUNT_OFFSET);
        }

    }

    VirtualMachine machine;
    int pageMap;
    int root;

    public SystemProtocol(VirtualMachine machine, int pageMap, int root) {
        this.machine = machine;
        this.pageMap = pageMap;
        this.root = root;
    }

    Id id() {
        return new Id();
    }

    Status status() {
        return new Status();
    }

    Space space() {
        return new Space();
    }

    Logicians logicians() {
        return new Logicians();
    }

    Connections connections() {
        return new Connections();
    }

    Messages inbox() {
        Messages messages = new Messages();
        messages.offset = INBOX_OFFSET;
        return messages;
    }

    Messages outbox() {
        Messages messages = new Messages();
        messages.offset = OUTBOX_OFFSET;
        return messages;
    }

    SystemStruct toStruct() {
        return new SystemStruct();
    }

    static class SystemStruct {

    }

}
