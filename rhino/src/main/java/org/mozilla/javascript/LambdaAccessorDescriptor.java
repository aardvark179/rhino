package org.mozilla.javascript;

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
            if (getter != null) {
                slot.throwNoSetterException(start, value);
                return true;
            }
        } else {
            setter.accept(start, value);
            return true;
        }

        return super.setValue(slot, value, start, start, isThrow);
    }

    @Override
    public CompactSlot<LambdaAccessorDescriptor<T>, Scriptable, T> createSlot(T owner, int attr) {
        var slot = new CompactSlot3<>(this, attr);
        return slot;
    }
}
