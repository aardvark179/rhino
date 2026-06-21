package org.mozilla.javascript;

import java.util.ArrayList;

public abstract class ArrayLiteralDescriptor {


    public abstract Scriptable createArray(Context cx, VarScope scope, ResultAccumulator results);

    public static class SimpleArrayLiteral extends ArrayLiteralDescriptor {

        @Override
        public Scriptable createArray(Context cx, VarScope scope, ResultAccumulator results) {
            return cx.newArray(scope, results.getResults());
        }
    }

    private static class ComplexArrayLiteral extends ArrayLiteralDescriptor {

        // List of skips
        final int[] skips;
        final int[] spreads;

        public ComplexArrayLiteral(int[] skips, int[] spreads) {
            this.skips = skips;
            this.spreads = spreads;
        }

        @Override
        public Scriptable createArray(Context cx, VarScope scope, ResultAccumulator results) {
            var elements = new ArrayList<Object>(results.getSize() * 2);
            int nextSkipPos = -1;
            int nextSpreadPos = -1;

            for (int i = 0; i < results.getSize(); i++) {
            }
            return null;
        }
    }
}
