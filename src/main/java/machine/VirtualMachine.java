package machine;

import java.util.*;

// TODO: move load/store into processor, it can handle pageMap stuff?
public class VirtualMachine {

    // Config - will move into constructor eventually
    public static final int PAGE_COUNT = 256;
    public static final int PAGE_SIZE = 4096;

    // Core state of the machine
    public List<Processor> processors;
    public byte[][] pages;

    public ArrayList<String> logs = new ArrayList<>();

    Notifiable interruptable;

    public VirtualMachine(int processorCount, Notifiable interruptable) {
        processors = new ArrayList<>(processorCount);
        pages = new byte[PAGE_COUNT][PAGE_SIZE];
        for (int i = 1; i <= processorCount; i++) {
            processors.add(new Processor(this));
        }
        this.interruptable = interruptable;
    }

    public void tick() {
        for (Processor p : processors) {
            p.tick();
        }
    }

    public byte[] load(int pageMapAddr, int address, byte[] bytes) {
        byte[] pageMap = pages[pageMapAddr];
        int pageNumber = (address / PAGE_SIZE);
        int addressInPage = address % PAGE_SIZE;

        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = pages[pageMap[pageNumber]][addressInPage];
            if (addressInPage == PAGE_SIZE - 1) {
                pageNumber++;
                addressInPage = 0;
            } else {
                addressInPage++;
            }
        }
        return bytes;
    }

    public void store(int pageMapAddr, int address, byte[] bytes) {
        byte[] pageMap = pages[pageMapAddr];
        int pageNumber = (address / PAGE_SIZE);
        int addressInPage = address % PAGE_SIZE;

        for (int i = 0; i < bytes.length; i++) {
            pages[pageMap[pageNumber]][addressInPage] = bytes[i];
            if (addressInPage == PAGE_SIZE - 1) {
                // beginning of next page
                pageNumber++;
                addressInPage = 0;
            } else {
                addressInPage++;
            }
        }
    }

    public byte loadByte(int pageMap, int address) {
        byte[] bytes = new byte[1];
        load(pageMap, address, bytes);
        return bytes[0];
    }

    public int loadInt(int pageMap, int address, int size) {
        byte[] bytes = new byte[size];
        load(pageMap, address, bytes);
        return bytesToInt(bytes);
    }

    public int loadInt(int pageMap, int address) {
        return loadInt(pageMap, address, 4);
    }

    public void store(int pageMap, int address, byte b) {
        store(pageMap, address, new byte[]{ b });
    }

    public void store(int pageMap, int address, int i) {
        store(pageMap, address, intToBytes(i));
    }

    public void store(int pageMap, int address, int i, int size) {
        store(pageMap, address, intToBytes(i, size));
    }

    public int bytesToInt(byte[] bytes) {
        assert bytes.length <= 4;
        int r = 0;
        for (int i = 0; i < bytes.length; i++) {
            int bi = bytes[i] & 0xFF;
            r = r | (bi << (8 * i));
        }
        return r;
    }

    public static byte[] intToBytes(int val) {
        byte[] bytes = new byte[4];
        for (int i = 0; i <= 3; i++) {
            bytes[i] = (byte) (val >>> (8 * i));
        }
        return bytes;
    }

    public static byte[] intToBytes(int val, int size) {
        byte[] bytes = new byte[size];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) (val >>> (8 * i));
        }
        return bytes;
    }

    class PageTableEntry {
        boolean valid;
        boolean read;
        boolean write;
        boolean execute;
        boolean user;
        boolean global;
        boolean accessed;
        boolean dirty;
        int ppn0;
        int ppn1;
        int ppn2;

        void at(long address) {

        }
    }

}
