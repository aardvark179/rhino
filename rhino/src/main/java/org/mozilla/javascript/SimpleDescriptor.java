package org.mozilla.javascript;

import org.mozilla.javascript.ScriptableObject.DescriptorInfo;

public class SimpleDescriptor<U extends PropHolder<U>, O extends SlotMapOwner<U>>
        extends CompactSlot.Descriptor<SimpleDescriptor<U, O>, U, O> {

    @SuppressWarnings("unchecked")
    static <U extends PropHolder<U>, O extends SlotMapOwner<U>>
            CompactSlot<SimpleDescriptor<U, O>, U, O> slotFrom(Slot<U> oldSlot) {
        if (oldSlot instanceof CompactSlot cs && cs.descriptor instanceof SimpleDescriptor desc) {
            return desc.createSlot(null, null, oldSlot.getAttributes());
        } else {
            return new SimpleDescriptor<U, O>(oldSlot.getName(), oldSlot.getIndexOrHash())
                    .createSlot(null, null, oldSlot.getAttributes());
        }
    }

    protected SimpleDescriptor(Object name, int indexOrHash) {
        super(name, indexOrHash);
    }

    @Override
    public CompactSlot<SimpleDescriptor<U, O>, U, O> createSlot(VarScope scope, O owner, int attr) {
        return new CompactSlot<SimpleDescriptor<U, O>, U, O>(this, attr);
    }

    @Override
    DescriptorInfo getPropertyDescriptor(
            CompactSlot<SimpleDescriptor<U, O>, U, O> slot, Context cx, U start) {
        return ScriptableObject.buildDataDescriptor(slot.getRawValue(), slot.getAttributes());
    }

    @Override
    public boolean isValueDescriptor() {
        return true;
    }

    @Override
    public boolean isSetterDescriptor() {
        return false;
    }

    public int reuiqredStorage() {
        return 1;
    }
}
