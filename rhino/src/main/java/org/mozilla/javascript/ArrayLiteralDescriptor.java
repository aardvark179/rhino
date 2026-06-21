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
            int skipIndex = skips.length > 0 ? 0 : -1;
            int spreadIndex = spreads.length > 0 ? 0 : -1;
            int nextSkipPos = skips.length > 0 ? skips[0] : -1;
            int nextSpreadPos = spreads.length > 0 ? spreads[0] : -1;

            int index = 0;
            for (int i = 0; i < results.getSize(); i++) {
                while (index == nextSkipPos) {
                    skipIndex++;
                    nextSkipPos = skips.length > skipIndex ? skips[skipIndex] : -1;
                    elements.add(Scriptable.NOT_FOUND);
                    index++;
                }
                if (index == nextSpreadPos) {
                    spreadIndex++;
                    nextSpreadPos = spreads.length > spreadIndex ? spreads[spreadIndex] : -1;
                    var e = results.getResult(i);
                    while (e != ObjectLiteralDescriptor.SPREAD_END) {
                        elements.add(e);
                        i++;
                        e = results.getResult(i);
                    }
                } else {
                    elements.add(results.getResult(i));
                }
                index++;
            }
            // Handle trailing skips
            while (index == nextSkipPos) {
                skipIndex++;
                nextSkipPos = skips.length > skipIndex ? skips[skipIndex] : -1;
                elements.add(Scriptable.NOT_FOUND);
                index++;
            }
            return cx.newArray(scope, elements.toArray());
        }
    }

    public static class Builder {
        private final ArrayList<Integer> skips = new ArrayList<>();
        private final ArrayList<Integer> spreads = new ArrayList<>();
        private int elements = 0;

        public Builder withElement() {
            elements++;
            return this;
        }

        public Builder withSkip() {
            skips.add(elements++);
            return this;
        }

        public Builder withSpread() {
            spreads.add(elements++);
            return this;
        }

        public ArrayLiteralDescriptor build() {
            if (skips.size() > 0 || spreads.size() > 0) {
                return new ComplexArrayLiteral(
                        skips.stream().mapToInt(v -> v.intValue()).toArray(),
                        spreads.stream().mapToInt(v -> v.intValue()).toArray());
            } else {
                return new SimpleArrayLiteral();
            }
        }
    }
}
