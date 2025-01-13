package system;

import machine.VirtualMachine;

public class IO {

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
            m.store(pageMap, root, current);
        }

        void commit() {
            current = root + 4;
        }

        void write(String val) {
            m.store(pageMap, current, val.length());
            current += 4;
            m.store(pageMap, current, val.getBytes());
            current += val.getBytes().length;
        }

        void write(int x) {
            m.store(pageMap, current, x);
            current += 4;
        }

        void write(byte[] bytes) {
            m.store(pageMap, current, bytes);
            current += bytes.length;
        }

    }

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

}
