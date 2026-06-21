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

    public int getSize() {
        return size;
    }

    public Object getResult(int i) {
        return results[i];
    }
}
