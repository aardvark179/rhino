package org.mozilla.javascript;

public class CompactSlot3<
                T extends CompactSlot.Descriptor<T, U, O>,
                U extends PropHolder<U>,
                O extends SlotMapOwner<U>>
        extends CompactSlot<T, U, O> {

    private Object value2;
    private Object value3;

    CompactSlot3(T descriptor, int attr) {
        super(descriptor, attr);
    }

    CompactSlot3(CompactSlot3<T, U, O> oldSlot) {
        super(oldSlot);
        this.value2 = oldSlot.value2;
        this.value3 = oldSlot.value3;
    }

    @Override
    Slot<U> copySlot() {
        return new CompactSlot3<>(this);
    }

    protected final Object getRawValue2() {
        return value2;
    }

    protected final void setRawValue2(Object value) {
        this.value2 = value;
    }

    protected final Object getRawValue3() {
        return value3;
    }

    protected final void setRawValue3(Object value) {
        this.value3 = value;
    }
}
