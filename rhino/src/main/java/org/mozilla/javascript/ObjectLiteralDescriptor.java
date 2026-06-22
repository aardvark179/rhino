package org.mozilla.javascript;

import java.util.ArrayList;
import org.mozilla.javascript.ScriptRuntime.StringIdOrIndex;

public abstract class ObjectLiteralDescriptor {
    // We expext the majority of object literals to be simple, only
    // associating values with keys. Method fall into this category
    // but may require some inference of function names. Any such
    // inference can be handled at compile time and should not be on
    // our hot path here.
    //
    // Computed properties require us to get the key and the value
    // from the evaluated list of expressions.
    //
    // Spread may be best handled in here by a `SPREAD` indicator, but
    // might be better done with a `SPREAD_START` and `SPREAD_END`,
    // it's going to depend a little on how complex the iterator
    // behaviour we need to tackle is in destructuring.
    //
    // Spread does seem to make more sense as emitting a start and end
    // token, and accumulating the results in between. This would
    // allow for a simple accumulation bytecode we can use for object
    // literals and other destructing type things. We don't even need
    // a spread start, because the use site should understand that it
    // is in the process of gathering spread results.
    //
    // SKIPPED indices need to be indicated in the list of ops, but
    // shouldn't be anything the interpreters need to worry about.

    public abstract Scriptable createObject(Context cx, VarScope scope, Object[] values);

    public void put(Scriptable obj, Object key, Object value) {
        if (key instanceof Symbol s) {
            obj.put(s, obj, value);
        } else if (key instanceof Integer i && i >= 0) {
            obj.put((int) i, obj, value);
        } else {
            obj.put(key.toString(), obj, value);
        }
    }

    public static class SimpleClassLitDescriptor extends ObjectLiteralDescriptor {
        private final Object[] keys;

        public SimpleClassLitDescriptor(Object[] keys) {
            this.keys = keys;
        }

        @Override
        public Scriptable createObject(Context cx, VarScope scope, Object[] values) {
            var obj = cx.newObject(scope);
            for (int i = 0; i < values.length; i++) {
                put(obj, keys[i], values[i]);
            }
            return obj;
        }
    }

    public static final Object COMPUTED_KEY = new Object();

    public static class ComputedKeyObjectLiteral extends ObjectLiteralDescriptor {
        private final Object[] ops;

        public ComputedKeyObjectLiteral(Object[] ops) {
            this.ops = ops;
        }

        @Override
        public Scriptable createObject(Context cx, VarScope scope, Object[] values) {
            var obj = cx.newObject(scope);
            int v = 0;
            for (int k = 0; k < ops.length; k++) {
                var key = ops[k] == COMPUTED_KEY ? values[v++] : ops[k];
                var value = values[v++];
                put(obj, key, value);
            }
            return obj;
        }
    }

    private abstract static class AccessorEntry {
        private final Object key;

        public AccessorEntry(Object key) {
            this.key = key;
        }
    }

    private static class SetterEntry extends AccessorEntry {
        public SetterEntry(Object key) {
            super(key);
        }
    }

    private static class GetterEntry extends AccessorEntry {
        public GetterEntry(Object key) {
            super(key);
        }
    }

    private static class SpreadEntry {}

    public static final Object SPREAD_END = new Object();

    public static class ComplexObjectLiteral extends ObjectLiteralDescriptor {
        private final Object[] ops;

        public ComplexObjectLiteral(Object[] ops) {
            this.ops = ops;
        }

        @Override
        public Scriptable createObject(Context cx, VarScope scope, Object[] values) {
            var obj = (ScriptableObject) cx.newObject(scope);
            int v = 0;
            for (int k = 0; k < ops.length; k++) {
                var key = ops[k] == COMPUTED_KEY ? values[v++] : ops[k];
                if (key instanceof AccessorEntry e) {
                    v = processAccessor(obj, e, values, v);
                } else if (key instanceof SpreadEntry) {
                    while (values[v] != SPREAD_END) {
                        key = values[v++];
                        var value = values[v++];
                        put(obj, key, value);
                    }
                } else {
                    var value = values[v++];
                    put(obj, key, value);
                }
            }
            return obj;
        }

        private int processAccessor(ScriptableObject obj, AccessorEntry e, Object[] values, int v) {
            var key = e.key == COMPUTED_KEY ? values[v++] : e.key;
            var value = (Callable) values[v++];
            boolean isSetter = e instanceof SetterEntry;
            if (ScriptRuntime.isSymbol(key)) {
                obj.setGetterOrSetter(key, 0, value, isSetter);
            } else if (key instanceof Integer && ((Integer) key) >= 0) {
                obj.setGetterOrSetter(null, (Integer) key, value, isSetter);
            } else {
                StringIdOrIndex s = ScriptRuntime.toStringIdOrIndex(key);
                obj.setGetterOrSetter(
                        s.getStringId(), s.getIndex() == -1 ? 0 : s.getIndex(), value, isSetter);
            }
            // Set the accessor slot bits as needed.
            return v;
        }
    }

    // Give ourselves a builder for the compiler to use

    public static class Builder {
        private ArrayList<Object> ops = new ArrayList<>();
        boolean hasKeys = false;
        boolean hasComputed = false;
        boolean hasGetterSetter = false;
        boolean hasSpread = false;

        public void addLiteralKey(Object k) {
            ops.add(k);
            hasKeys = true;
        }

        public void addComputedKey() {
            ops.add(COMPUTED_KEY);
            hasComputed = true;
        }

        public void addGetter(Object s) {
            ops.add(new GetterEntry(s));
            hasGetterSetter = true;
        }

        public void addSetter(Object s) {
            ops.add(new SetterEntry(s));
            hasGetterSetter = true;
        }

        public void addComputedGetter() {
            ops.add(new GetterEntry(COMPUTED_KEY));
            hasGetterSetter = true;
        }

        public void addComputedSetter() {
            ops.add(new SetterEntry(COMPUTED_KEY));
            hasGetterSetter = true;
        }

        public void addSpread() {
            ops.add(new SpreadEntry());
            hasSpread = true;
        }

        public ObjectLiteralDescriptor build() {
            if (hasSpread || hasGetterSetter) {
                return new ComplexObjectLiteral(ops.toArray());
            } else if (hasComputed) {
                return new ComputedKeyObjectLiteral(ops.toArray());
            } else {
                return new SimpleClassLitDescriptor(ops.toArray());
            }
        }
    }
}
