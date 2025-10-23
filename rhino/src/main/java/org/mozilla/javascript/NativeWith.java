/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

/**
 * This class implements the object lookup required for the {@code with} statement. It simply
 * delegates every action to its object except for operations on its parent.
 */
public class NativeWith extends NativeScope {
    private static final long serialVersionUID = 1L;

    private static final Object WITH_TAG = new Object();

    static void init(JSScope scope, boolean sealed) {

        LambdaConstructor ctor = new LambdaConstructor(scope, "with", 0, NativeWith::js_construct);

        if (sealed) {
            ctor.sealObject();
        }
        ctor.associateValue(WITH_TAG, WITH_TAG);
        scope.put("With", scope, ctor);
    }

    protected NativeWith(JSScope parent, Scriptable object) {
        super(parent);
        this.object = object;
    }

    @Override
    public boolean has(String id, JSScope start) {
        return object.has(id, object);
    }

    @Override
    public boolean has(Symbol key, JSScope start) {
        if (object instanceof SymbolScriptable) {
            return ((SymbolScriptable) object).has(key, object);
        }
        return false;
    }

    @Override
    public boolean has(int index, JSScope start) {
        return object.has(index, object);
    }

    @Override
    public Object get(String id, JSScope start) {
        if (start == this) {
            start = object;
        }
        return object.get(id, start);
    }

    @Override
    public Object get(Symbol key, JSScope start) {
        if (start == this) {
            start = object;
        }
        if (object instanceof SymbolScriptable) {
            return ((SymbolScriptable) object).get(key, start);
        }
        return Scriptable.NOT_FOUND;
    }

    @Override
    public Object get(int index, JSScope start) {
        if (start == this) {
            start = object;
        }
        return object.get(index, start);
    }

    @Override
    public void put(String id, JSScope start, Object value) {
        if (start == this) start = object;
        object.put(id, start, value);
    }

    @Override
    public void put(Symbol symbol, JSScope start, Object value) {
        if (start == this) {
            start = object;
        }
        if (object instanceof SymbolScriptable) {
            ((SymbolScriptable) object).put(symbol, start, value);
        }
    }

    @Override
    public void put(int index, JSScope start, Object value) {
        if (start == this) start = object;
        object.put(index, start, value);
    }

    @Override
    public void delete(String id) {
        object.delete(id);
    }

    @Override
    public void delete(Symbol key) {
        if (object instanceof SymbolScriptable) {
            ((SymbolScriptable) object).delete(key);
        }
    }

    @Override
    public void delete(int index) {
        object.delete(index);
    }

    public Scriptable getObject() {
        return object;
    }

    public void setObject(Scriptable object) {
        this.object = object;
    }

    /** Must return null to continue looping or the final collection result. */
    protected Object updateDotQuery(boolean value) {
        // NativeWith itself does not support it
        throw new IllegalStateException();
    }

    private static Scriptable js_construct(
            Context cx, JSScope scope, Object target, Object[] args) {
        throw Context.reportRuntimeErrorById("msg.cant.call.indirect", "With");
    }

    static boolean isWithFunction(Object functionObj) {
        if (functionObj instanceof LambdaConstructor) {
            return ((LambdaConstructor) functionObj).getAssociatedValue(WITH_TAG) == WITH_TAG;
        }
        return false;
    }

    static Object newWithSpecial(Context cx, JSScope scope, Object[] args) {
        ScriptRuntime.checkDeprecated(cx, "With");
        scope = ScriptableObject.getTopLevelScope(scope);
        Scriptable proto =
                args.length == 0
                        ? ScriptableObject.getObjectPrototype(scope)
                        : ScriptRuntime.toObject(cx, scope, args[0]);
        return new NativeWith(scope, proto);
    }

    @Override
    public boolean isBoundaryScope() {
        return false;
    }

    private static final Object FTAG = "With";

    private static final int Id_constructor = 1;

    protected Scriptable object;
}
