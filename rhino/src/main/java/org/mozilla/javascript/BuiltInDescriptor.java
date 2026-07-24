package org.mozilla.javascript;

import java.io.Serial;
import java.io.Serializable;
import org.mozilla.javascript.ScriptableObject.DescriptorInfo;

public class BuiltInDescriptor<T extends ScriptableObject>
        extends CompactSlot.Descriptor<BuiltInDescriptor<T>, Scriptable, T> {
    @Serial private static final long serialVersionUID = 8728562620206845355L;

    public interface Getter<T extends ScriptableObject> extends Serializable {
        Object apply(T builtIn, Scriptable start);
    }

    public interface Setter<T extends ScriptableObject> extends Serializable {
        boolean apply(T builtIn, Object value, Scriptable owner, Scriptable start, boolean isThrow);
    }

    public interface AttributeSetter<U extends ScriptableObject> extends Serializable {
        void apply(U builtIn, int attributes);
    }

    public interface PropDescriptionSetter<T extends ScriptableObject> extends Serializable {
        boolean apply(
                T builtIn,
                CompactSlot<BuiltInDescriptor<T>, Scriptable, T> current,
                Object id,
                DescriptorInfo info,
                boolean checkValid,
                Object key,
                int index);
    }

    public static <T extends ScriptableObject> BuiltInDescriptor<T> builtInDesc(
            Object name, Getter<T> getter) {
        return new BuiltInDescriptor<T>(
                name,
                0,
                getter,
                BuiltInDescriptor::defaultSetter,
                BuiltInDescriptor::defaultAttrSetter,
                BuiltInDescriptor::defaultPropDescSetter);
    }

    public static <T extends ScriptableObject> BuiltInDescriptor<T> builtInDesc(
            Object name, Getter<T> getter, Setter<T> setter) {
        return new BuiltInDescriptor<>(
                name,
                0,
                getter,
                setter,
                BuiltInDescriptor::defaultAttrSetter,
                BuiltInDescriptor::defaultPropDescSetter);
    }

    public static <T extends ScriptableObject> BuiltInDescriptor<T> builtInDesc(
            Object name, Getter<T> getter, Setter<T> setter, AttributeSetter<T> attrUpdater) {
        return new BuiltInDescriptor<>(
                name, 0, getter, setter, attrUpdater, BuiltInDescriptor::defaultPropDescSetter);
    }

    public static <T extends ScriptableObject> BuiltInDescriptor<T> builtInDesc(
            Object name,
            Getter<T> getter,
            Setter<T> setter,
            AttributeSetter<T> attrUpdater,
            PropDescriptionSetter<T> propDescSetter) {
        return new BuiltInDescriptor<>(name, 0, getter, setter, attrUpdater, propDescSetter);
    }

    /* When setting a property descriptor we need to set the property
    _without_ the normal checks on readonly and similar. */
    @SuppressWarnings("unchecked")
    static <T extends ScriptableObject> void setValueFromDescriptor(
            CompactSlot<BuiltInDescriptor<T>, Scriptable, T> slot,
            Object value,
            Scriptable owner,
            Scriptable start,
            boolean isThrow) {
        slot.descriptor.setter.apply(((T) slot.value), value, owner, start, isThrow);
    }

    static boolean isBuiltIn(Slot<?> slot) {
        return slot instanceof CompactSlot<?, ?, ?> cs
                && cs.descriptor instanceof BuiltInDescriptor;
    }

    @SuppressWarnings("unchecked")
    static <T extends ScriptableObject> boolean applyNewDescriptor(
            CompactSlot<BuiltInDescriptor<T>, Scriptable, T> slot,
            Object id,
            DescriptorInfo info,
            boolean checkValid,
            Object key,
            int index) {
        return slot.descriptor.propDescSetter.apply(
                ((T) slot.value), slot, id, info, checkValid, key, index);
    }

    private static <T extends ScriptableObject> boolean defaultSetter(
            T builtIn, Object value, Scriptable owner, Scriptable start, boolean isThrow) {
        return true;
    }

    private static <T extends ScriptableObject> void defaultAttrSetter(T builtIn, int attributes) {
        // Do nothing.
    }

    private static <T extends ScriptableObject> boolean defaultPropDescSetter(
            T builtIn,
            CompactSlot<BuiltInDescriptor<T>, Scriptable, T> current,
            Object id,
            DescriptorInfo info,
            boolean checkValid,
            Object key,
            int index) {
        try (var map = builtIn.startCompoundOp(true)) {
            return ScriptableObject.defineOrdinaryProperty(
                    ScriptableObject::setSlotValue, builtIn, map, id, info, checkValid, key, index);
        }
    }

    private final Getter<T> getter;
    private final Setter<T> setter;
    private final AttributeSetter<T> attrUpdater;
    private final PropDescriptionSetter<T> propDescSetter;

    public BuiltInDescriptor(
            Object name,
            int indexOrHash,
            Getter<T> getter,
            Setter<T> setter,
            AttributeSetter<T> attrUpdater,
            PropDescriptionSetter<T> propDescSetter) {
        super(name, indexOrHash);
        this.getter = getter;
        this.setter = setter;
        this.attrUpdater = attrUpdater;
        this.propDescSetter = propDescSetter;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object getValue(
            CompactSlot<BuiltInDescriptor<T>, Scriptable, T> slot, Scriptable start) {
        return getter.apply(((T) slot.getRawValue()), start);
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean setValue(
            CompactSlot<BuiltInDescriptor<T>, Scriptable, T> slot,
            Object value,
            Scriptable owner,
            Scriptable start,
            boolean isThrow) {
        if ((slot.getAttributes() & ScriptableObject.READONLY) != 0) {
            if (isThrow) {
                throw ScriptRuntime.typeErrorById("msg.modify.readonly", getName());
            }
            return true;
        }
        if (owner == start) {
            return setter.apply(((T) slot.getRawValue()), value, owner, start, isThrow);
        }
        return false;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setAttributes(CompactSlot<BuiltInDescriptor<T>, Scriptable, T> slot, int value) {
        attrUpdater.apply(((T) slot.getRawValue()), value);
        super.setAttributes(slot, value);
    }

    @Override
    public CompactSlot<BuiltInDescriptor<T>, Scriptable, T> createSlot(T owner, int attr) {
        var slot = new CompactSlot<>(this, attr);
        slot.setRawValue(owner);
        return slot;
    }
}
