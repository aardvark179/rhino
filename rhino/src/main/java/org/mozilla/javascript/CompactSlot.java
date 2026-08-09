package org.mozilla.javascript;

public abstract class CompactSlot<T extends PropHolder<T>, O extends ScriptableObject>
        extends Slot<T> {

    public abstract static class Descriptor<
            T extends CompactSlot<U, O>, U extends PropHolder<U>, O extends ScriptableObject> {

        public abstract T createSlot(O owner, int attr);
    }

    private short attributes;

    CompactSlot(int attr) {
        super();
        this.attributes = (short) attr;
    }

    CompactSlot(CompactSlot<T, O> oldSlot) {
        super();
        this.attributes = oldSlot.attributes;
    }

    @Override
    int getAttributes() {
        return attributes;
    }

    @Override
    void setAttributes(int value) {
        this.attributes = (short) value;
    }
}
