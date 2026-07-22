package org.mozilla.javascript;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Objects;

public abstract class CompactSlot<T extends CompactSlot.Descriptor<T, U, O>, U extends PropHolder<U>, O extends ScriptableObject>
        extends Slot<U> {

    public abstract static class Descriptor<
            T extends CompactSlot.Descriptor<T, U, O>, U extends PropHolder<U>, O extends ScriptableObject>
implements Serializable {
        private final Object name;
        private int indexOrHash;

        protected Descriptor(Object name, int indexOrHash) {
            this.name = name;
            this.indexOrHash = name == null ? indexOrHash : name.hashCode();
        }

        public Object getName() {
            return name;
        }

        public int getIndexOrHash() {
            return indexOrHash;
        }

        public abstract CompactSlot<T, U, O> createSlot(O owner, int attr);

        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            in.defaultReadObject();
            if (name != null) {
                indexOrHash = name.hashCode();
            }
        }
    }

    protected final T descriptor;

    CompactSlot(T descriptor, int attr) {
        super(attr);
        this.descriptor = descriptor;
    }

    CompactSlot(CompactSlot<T, U, O> oldSlot) {
        super(oldSlot);
        this.descriptor = oldSlot.descriptor;
    }

    @Override
    public final boolean keyMatches(Object key, int indexOrHash) {
        return indexOrHash == this.descriptor.getIndexOrHash()
                && Objects.equals(this.descriptor.getName(), key);
    }

    @Override
    public final Object getKey() {
        return descriptor.getName() != null ? descriptor.getName() : descriptor.getIndexOrHash();
    }

    @Override
    public final Object getName() {
        return descriptor.getName();
    }

    @Override
    public final int getIndexOrHash() {
        return descriptor.getIndexOrHash();
    }
}
