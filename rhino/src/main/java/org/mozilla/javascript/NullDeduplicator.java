package org.mozilla.javascript;

import java.util.List;

public class NullDeduplicator implements Deduplicator {
    public static final NullDeduplicator INSTANCE = new NullDeduplicator();

    private NullDeduplicator() {}

    @Override
    public byte[] dedplicate(byte[] array) {
        return array;
    }

    @Override
    public String deduplicate(String s) {
        return s;
    }

    @Override
    public String[] deduplicate(String[] s) {
        return s;
    }

    @Override
    public List<String> deduplicate(List<String> s) {
        return s;
    }
}
