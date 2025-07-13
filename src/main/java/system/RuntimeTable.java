package system;

import java.util.ArrayList;

public class RuntimeTable {

    final int ENTRY_SIZE = 8;

    static class SubTable {
        TableType type;
        String name;
    }

    static class Entry {
        SubTable subTable;
        EntryType type;
        String name;
        int position;
        int primary;
        int secondary;
    }

    enum TableType {
        Document,
        Interface
    }

    enum EntryType {
        Document        ("size",    "addr"),
        Method          ("size",    "addr"),
        Field           ("size",    "addr"),
        Interface       ("size",    "addr"),
        Constant        ("size",    "addr"),
        ProtocolMethod  ("id",      "N/A"),
        System          ("pageSize","addr"),
        Error           ("N/A",     "N/A");

        final String primaryLabel;
        final String secondaryLabel;

        EntryType(String primaryLabel, String secondaryLabel) {
            this.primaryLabel = primaryLabel;
            this.secondaryLabel = secondaryLabel;
        }
    }

    ArrayList<Entry> entries = new ArrayList<>();

    static Builder builder() {
        RuntimeTable rt = new RuntimeTable();
        return rt.new Builder();
    }

    public int[] values() {
        int[] values = new int[entries.size() * 2];
        int index = 0;
        for (Entry entry : entries) {
            values[index] = entry.primary;
            values[index + 1] = entry.secondary;
            index += 2;
        }
        return values;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        int paddingLength = 5;
        SubTable current = null;
        int byteIndx = 0;
        for (Entry entry : entries) {
            if (entry.subTable != current) {
                current = entry.subTable;

                sb.append(current.type)
                        .append(": ")
                        .append(current.name)
                        .append("\n");
            }

            sb.repeat(" ", paddingLength - String.valueOf(byteIndx).length())
                .append(byteIndx)
                .append(": ")
                .repeat(" ", paddingLength - String.valueOf(entry.primary).length())
                .append(entry.primary)
                .append("    ~")
                .append(" ")
                .append(entry.type.name())
                .append(" ")
                .append(entry.type.primaryLabel)
                .append(" ")
                .append(entry.name)
                .append("\n");
            byteIndx += 4;
            sb.repeat(" ", paddingLength - String.valueOf(byteIndx).length())
                .append(byteIndx)
                .append(": ")
                .repeat(" ", paddingLength - String.valueOf(entry.secondary).length())
                .append(entry.secondary)
                .append("    ~")
                .append(" ")
                .append(entry.type.name())
                .append(" ")
                .append(entry.type.secondaryLabel)
                .append(" ")
                .append(entry.name)
                .append("\n");
            byteIndx += 4;
        }
        return sb.toString();
    }

    class Builder {

        SubTable current;

        void table(TableType type, String name) {
            SubTable subTable = new SubTable();
            subTable.type = type;
            subTable.name = name;
            current = subTable;
        }

        void entry(EntryType type, String name, int primary, int secondary) {
            int byteIndx = entries.size() * ENTRY_SIZE;
            Entry entry = new Entry();
            entry.subTable = current;
            entry.type = type;
            entry.name = name;
            entry.position = byteIndx;
            entry.primary = primary;
            entry.secondary = secondary;
            entries.add(entry);
        }

        RuntimeTable table() {
            return RuntimeTable.this;
        }

    }
}
