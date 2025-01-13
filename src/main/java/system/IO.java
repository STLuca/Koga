package system;

import machine.VirtualMachine;

public class IO {

    // Rename SystemIn, MemberIn?
    static class MemberOut {

        int pageMap;
        int root;
        int current;
        VirtualMachine m;

        public MemberOut(int pageMap, int addr, VirtualMachine m) {
            this.m = m;
            this.pageMap = pageMap;
            this.root = addr;
            this.current = root + 4;
            commit();
        }

        void commit() {
            m.store(pageMap, root, current);
        }

        void write(String val) {
            m.store(pageMap, current, val.length());
            current += 4;
            m.store(pageMap, current, val.getBytes());
            current += val.getBytes().length;
            commit();
        }

        void write(int x) {
            m.store(pageMap, current, x);
            current +=4;
            commit();
        }

        void write(byte[] bytes) {
            m.store(pageMap, current, bytes);
            current += bytes.length;
            commit();
        }

    }

    // Rename SystemOut, MemberIn?
    static class MemberIn {

        int pageMap;
        int root;
        int current;
        VirtualMachine m;

        public MemberIn(int pageMap, int addr, VirtualMachine m) {
            this.m = m;
            this.pageMap = pageMap;
            this.root = addr;
            this.current = root + 4;
            m.store(pageMap, root, current);
        }

        void update() {
            current = m.loadInt(pageMap, root);
        }

        int peekInt() {
            int result = m.loadInt(pageMap, current);
            return result;
        }

        int readInt() {
            int result = m.loadInt(pageMap, current);
            current = current + 4;
            return result;
        }

        String readString() {
            int size = m.loadInt(pageMap, current);
            current += 4;
            byte[] bytes = new byte[size];
            m.load(pageMap, current, bytes);
            return new String(bytes);
        }

    }

    static class NewMemberOut {

        int pageMap;
        int root;
        VirtualMachine m;

        int freeSlot = 1;

        public NewMemberOut(int pageMap, int addr, VirtualMachine m) {
            this.m = m;
            this.pageMap = pageMap;
            this.root = addr;
        }

        void connectRequest(int pageCount, int[] pages) {
            if (pages.length > 5) throw new RuntimeException();
            int index = freeSlot++;
            int addr = root + (index * 64);
            m.store(pageCount, addr, pageCount);
            addr+=4;
            for (int i : pages) {
                m.store(pageCount, addr, i);
                addr += 4;
            }
        }

    }

    static class NewMemberIn {

        int pageMap;
        int root;
        VirtualMachine m;

        public NewMemberIn(int pageMap, int addr, VirtualMachine m) {
            this.m = m;
            this.pageMap = pageMap;
            this.root = addr;
        }

        int read(int index, int secondIndex) {
            int addr = root + (index * 64);
            return m.loadInt(pageMap, addr + secondIndex);
        }

    }

}
