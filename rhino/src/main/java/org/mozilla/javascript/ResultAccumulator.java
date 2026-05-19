package org.mozilla.javascript;

public class ResultAccumulator {
    private Object[] results;
    private int size = 0;

    public ResultAccumulator(int capacity) {
        results = new Object[capacity];
    }

    public void addResult(Object result) {
        if (size == results.length) {
            var newResults = new Object[results.length * 2];
            System.arraycopy(results, 0, newResults, 0, results.length);
            results = newResults;
        }
        results[size++] = result;
    }

    public Object[] getResults() {
        if (size == results.length) {
            return results;
        } else {
            var res = new Object[size];
            System.arraycopy(results, 0, res, 0, size);
            return res;
        }
    }

    /**
     * Returns the accumulated values as a flat argument array, dropping the {@link
     * ObjectLiteralDescriptor#SPREAD_END} sentinels appended by {@link
     * ScriptRuntime#accumulateIteratorValues}. Used to turn accumulated call arguments (which,
     * unlike array literals, have no elisions) into a plain argument array.
     */
    public Object[] getCallArgs() {
        int n = 0;
        for (int i = 0; i < size; i++) {
            if (results[i] != ObjectLiteralDescriptor.SPREAD_END) {
                n++;
            }
        }
        if (n == 0) {
            return ScriptRuntime.emptyArgs;
        }
        Object[] args = new Object[n];
        int j = 0;
        for (int i = 0; i < size; i++) {
            Object v = results[i];
            if (v != ObjectLiteralDescriptor.SPREAD_END) {
                args[j++] = v;
            }
        }
        return args;
    }

    public int getSize() {
        return size;
    }

    public Object getResult(int i) {
        return results[i];
    }
}
