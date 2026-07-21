package org.mozilla.javascript;

import java.util.List;

public interface Deduplicator {
    String deduplicate(String s);

    default String[] deduplicate(String[] s) {
        for (int i = 0; i < s.length; i++) {
            s[i] = deduplicate(s[i]);
        }
        return s;
    }

    default List<String> deduplicate(List<String> s) {
        return s.stream().map(str -> deduplicate(str)).toList();
    }

    byte[] dedplicate(byte[] array);
}
