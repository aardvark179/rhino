package org.mozilla.javascript;

import org.mozilla.javascript.ScriptableObject.DescriptorInfo;

public class LambdaAccessorDescriptor<T extends ScriptableObject>
        extends CompactSlot.Descriptor<LambdaAccessorDescriptor<T>, Scriptable, T> {

    private final ScriptableObject.LambdaGetterFunction getter;
    private final ScriptableObject.LambdaSetterFunction setter;

    public LambdaAccessorDescriptor(
            Object name,
            int indexOrHash,
            ScriptableObject.LambdaGetterFunction getter,
            ScriptableObject.LambdaSetterFunction setter) {
        super(name, indexOrHash);
        this.getter = getter;
        this.setter = setter;
    }

    @Override
    public Object getValue(
            CompactSlot<LambdaAccessorDescriptor<T>, Scriptable, T> slot, Scriptable start) {
        if (getter != null) {
            return getter.apply(start);
        }
        return super.getValue(slot, start);
    }

    @Override
    public boolean setValue(
            CompactSlot<LambdaAccessorDescriptor<T>, Scriptable, T> slot,
            Object value,
            Scriptable owner,
            Scriptable start,
            boolean isThrow) {
        if (setter == null) {
            slot.throwNoSetterException(start, value);
        } else {
            setter.accept(start, value);
        }
        return true;
    }

    @Override
    public DescriptorInfo getPropertyDescriptor(
            CompactSlot<LambdaAccessorDescriptor<T>, Scriptable, T> slot,
            Context cx,
            Scriptable start) {
        var cs3 = (CompactSlot3<LambdaAccessorDescriptor<T>, Scriptable, T>) slot;
        int attr = slot.getAttributes();
        DescriptorInfo desc;
        boolean es6 = cx.getLanguageVersion() >= Context.VERSION_ES6;

        desc = new DescriptorInfo(ScriptableObject.NOT_FOUND, attr, false);
        desc.getter = cs3.getRawValue2();
        desc.setter = setter != null ? cs3.getRawValue3() : Undefined.instance;

        if (es6) {
            desc.enumerable = (attr & ScriptableObject.DONTENUM) == 0;
            desc.configurable = (attr & ScriptableObject.PERMANENT) == 0;
        }
        return desc;
    }

    @Override
    public boolean isValueDescriptor() {
        return false;
    }

    @Override
    public boolean isSetterDescriptor() {
        return true;
    }

    @Override
    public CompactSlot<LambdaAccessorDescriptor<T>, Scriptable, T> createSlot(
            VarScope scope, T owner, int attr) {
        var slot = new CompactSlot3<>(this, attr);
        slot.setRawValue2(
                new LambdaFunction(
                        scope,
                        "get " + super.getName(),
                        0,
                        (cx1, scope1, thisObj, args) -> getter.apply((Scriptable) thisObj),
                        false));
        if (setter != null) {
            slot.setRawValue3(
                    new LambdaFunction(
                            scope,
                            "set " + super.getName(),
                            1,
                            (cx1, scope1, thisObj, args) -> {
                                setter.accept(
                                        (Scriptable) thisObj,
                                        args.length > 0 ? args[0] : Undefined.instance);
                                return Undefined.instance;
                            },
                            false));
        }
        return slot;
    }

    public int reuiqredStorage() {
        return 3;
    }
}
