package org.mozilla.javascript.optimizer;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Reads the exception table of each method from a class file. */
final class ExceptionTableReader {

    record Entry(int start, int end, int handler, String type) {
        boolean covers(int pc) {
            return start <= pc && pc < end;
        }
    }

    private ExceptionTableReader() {}

    static Map<String, List<Entry>> read(byte[] classFile) {
        try {
            return read(new DataInputStream(new ByteArrayInputStream(classFile)));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Map<String, List<Entry>> read(DataInputStream in) throws IOException {
        in.readInt(); // magic
        in.readUnsignedShort(); // minor
        in.readUnsignedShort(); // major
        int poolSize = in.readUnsignedShort();
        String[] utf8 = new String[poolSize];
        int[] classNames = new int[poolSize];
        for (int i = 1; i < poolSize; i++) {
            int tag = in.readUnsignedByte();
            switch (tag) {
                case 1:
                    utf8[i] = in.readUTF();
                    break;
                case 7:
                    classNames[i] = in.readUnsignedShort();
                    break;
                case 8:
                case 16:
                case 19:
                case 20:
                    in.readUnsignedShort();
                    break;
                case 15:
                    in.readUnsignedByte();
                    in.readUnsignedShort();
                    break;
                case 3:
                case 4:
                case 9:
                case 10:
                case 11:
                case 12:
                case 17:
                case 18:
                    in.readInt();
                    break;
                case 5:
                case 6:
                    in.readLong();
                    i++;
                    break;
                default:
                    throw new IllegalStateException("Unknown constant pool tag " + tag);
            }
        }
        in.readUnsignedShort(); // access
        in.readUnsignedShort(); // this
        in.readUnsignedShort(); // super
        int interfaces = in.readUnsignedShort();
        for (int i = 0; i < interfaces; i++) {
            in.readUnsignedShort();
        }
        int fields = in.readUnsignedShort();
        for (int i = 0; i < fields; i++) {
            in.readUnsignedShort();
            in.readUnsignedShort();
            in.readUnsignedShort();
            skipAttributes(in);
        }

        Map<String, List<Entry>> result = new LinkedHashMap<>();
        int methods = in.readUnsignedShort();
        for (int i = 0; i < methods; i++) {
            in.readUnsignedShort();
            String name = utf8[in.readUnsignedShort()];
            in.readUnsignedShort();
            List<Entry> entries = new ArrayList<>();
            int attributes = in.readUnsignedShort();
            for (int j = 0; j < attributes; j++) {
                String attrName = utf8[in.readUnsignedShort()];
                int length = in.readInt();
                if (!"Code".equals(attrName)) {
                    in.skipNBytes(length);
                    continue;
                }
                in.readUnsignedShort(); // max stack
                in.readUnsignedShort(); // max locals
                in.skipNBytes(in.readInt());
                int tableLength = in.readUnsignedShort();
                for (int k = 0; k < tableLength; k++) {
                    int start = in.readUnsignedShort();
                    int end = in.readUnsignedShort();
                    int handler = in.readUnsignedShort();
                    int catchType = in.readUnsignedShort();
                    String type = catchType == 0 ? null : utf8[classNames[catchType]];
                    entries.add(new Entry(start, end, handler, type));
                }
                skipAttributes(in);
            }
            result.put(name, entries);
        }
        return result;
    }

    private static void skipAttributes(DataInputStream in) throws IOException {
        int count = in.readUnsignedShort();
        for (int i = 0; i < count; i++) {
            in.readUnsignedShort();
            in.skipNBytes(in.readInt());
        }
    }
}
