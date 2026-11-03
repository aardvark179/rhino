package org.mozilla.javascript;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Runtime descriptor for an ES6 class literal, playing the same role for classes that {@link
 * ObjectLiteralDescriptor} plays for object literals: it is built once at parse time and describes
 * the class's shape, and is invoked identically from all three backends via a single {@link
 * #createClass} call.
 *
 * <p>{@code values[0]} is always the (already-built) constructor function. The rest of {@code
 * values} holds, per member entry in order: the computed key value (only for entries whose key is
 * {@link UniqueTag#COMPUTED_KEY}) followed by the member's function value. After the members come
 * the computed key values of any instance fields without initializers that have computed keys and,
 * if any field has an initializer, a single field initializer function, which creates the fields
 * with initializers on its {@code this} in declaration order. {@code prototype} is a freshly
 * created plain object (built the same way as an object literal's {@code EMPTY_OBJECT}) that
 * becomes the constructor's {@code .prototype}; methods/the constructor are all built with {@code
 * homeObject == prototype} via the same "peek the object under construction" convention
 * object-literal methods already use.
 *
 * <p>Instance fields are not installed by {@link #createClass}: the field initializer and the keys
 * of the fields without initializers are recorded on the constructor, and {@link #initializeFields}
 * defines them on each new instance once its {@code this} value is available - at the start of a
 * base class constructor, or after the {@code super(...)} call of a derived one.
 */
public abstract class ClassLiteralDescriptor implements Serializable {

    public abstract Scriptable createClass(
            Context cx, VarScope scope, Object superClass, Scriptable prototype, Object[] values);

    /**
     * Initializes the instance fields of class constructor {@code constructor} on {@code thisObj},
     * and returns {@code thisObj}: first the fields with initializers, by calling the field
     * initializer, and then the rest, as {@code undefined}. The computed keys and field initializer
     * captured when the class was evaluated are held by the constructor.
     */
    public Object initializeFields(Context cx, VarScope scope, Object constructor, Object thisObj) {
        return thisObj;
    }

    /**
     * Wires up the constructor/prototype relationship described in ES2015 15.7.14
     * ClassDefinitionEvaluation: the constructor's own parent (for static inheritance and {@code
     * super()} resolution), the prototype's parent (for instance method/`super.x` inheritance), the
     * constructor's {@code .prototype} property, and the prototype's non-enumerable {@code
     * constructor} back-reference.
     */
    static BaseFunction setupClass(
            BaseFunction constructor, Scriptable prototype, Object superClass) {
        if (!Undefined.isUndefined(superClass)) {
            Scriptable superProto;
            if (superClass == null) {
                superProto = null;
            } else if (superClass instanceof Constructable && superClass instanceof Scriptable) {
                Scriptable superClassObj = (Scriptable) superClass;
                Object proto = superClassObj.get("prototype", superClassObj);
                superProto = (proto instanceof Scriptable) ? (Scriptable) proto : null;
                constructor.setPrototype(superClassObj);
            } else {
                throw ScriptRuntime.typeErrorById("msg.extends.not.ctor");
            }
            if (prototype instanceof ScriptableObject) {
                ((ScriptableObject) prototype).setPrototype(superProto);
            }
        }

        constructor.setPrototypeProperty(prototype);
        if (prototype instanceof ScriptableObject) {
            ((ScriptableObject) prototype)
                    .defineProperty("constructor", constructor, ScriptableObject.DONTENUM);
        }
        return constructor;
    }

    /** Installs {@code members} on {@code target}, returning the index of the next unused value. */
    private static int installMembers(
            ScriptableObject target, MemberEntry[] members, Object[] values) {
        int v = 1;
        for (MemberEntry member : members) {
            Object key = member.key == UniqueTag.COMPUTED_KEY ? values[v++] : member.key;
            Object value = values[v++];
            installMember(target, key, member.kind, value);
        }
        return v;
    }

    private static void defineField(Object key, Object value, Context cx, Scriptable thisObj) {
        if (!(thisObj instanceof ScriptableObject)) {
            ScriptRuntime.setObjectElem(thisObj, key, value, cx);
            return;
        }
        ScriptableObject so = (ScriptableObject) thisObj;
        if (key instanceof Symbol) {
            so.defineProperty((Symbol) key, value, ScriptableObject.EMPTY);
            return;
        }
        var s = ScriptRuntime.toStringIdOrIndex(key);
        if (s.stringId != null) {
            so.defineProperty(s.stringId, value, ScriptableObject.EMPTY);
        } else {
            so.put(s.index, so, value);
            so.setAttributes(s.index, ScriptableObject.EMPTY);
        }
    }

    private static void installMember(
            ScriptableObject target, Object key, ElementKind kind, Object value) {
        boolean isSetter = kind == ElementKind.SETTER;
        if (key instanceof Symbol) {
            // Includes private names (SymbolKey, Symbol.Kind.PRIVATE): already hidden from
            // Object.keys/for-in like any Symbol-keyed property (see SlotMapOwner.getIds).
            Symbol symbol = (Symbol) key;
            if (kind == ElementKind.METHOD) {
                target.put(symbol, target, value);
                target.setAttributes(symbol, ScriptableObject.DONTENUM);
            } else {
                target.setGetterOrSetter(symbol, 0, (Callable) value, isSetter);
                target.setAttributes(
                        symbol,
                        (target.getAttributes(symbol) | ScriptableObject.DONTENUM)
                                & ~ScriptableObject.READONLY);
            }
            return;
        }
        var s = ScriptRuntime.toStringIdOrIndex(key);
        if (kind == ElementKind.METHOD) {
            if (s.stringId != null) {
                target.defineProperty(s.stringId, value, ScriptableObject.DONTENUM);
            } else {
                target.put(s.index, target, value);
                target.setAttributes(s.index, ScriptableObject.DONTENUM);
            }
            return;
        }
        Callable accessor = (Callable) value;
        if (s.stringId != null) {
            target.setGetterOrSetter(s.stringId, 0, accessor, isSetter);
            target.setAttributes(
                    s.stringId,
                    (target.getAttributes(s.stringId) | ScriptableObject.DONTENUM)
                            & ~ScriptableObject.READONLY);
        } else {
            target.setGetterOrSetter(null, s.index, accessor, isSetter);
            target.setAttributes(
                    s.index,
                    (target.getAttributes(s.index) | ScriptableObject.DONTENUM)
                            & ~ScriptableObject.READONLY);
        }
    }

    /** Kind of a class member that is a function: normal method, getter, or setter. */
    public enum ElementKind {
        METHOD,
        GETTER,
        SETTER
    }

    public static final class MemberEntry implements Serializable {
        final Object key;
        final ElementKind kind;

        MemberEntry(Object key, ElementKind kind) {
            this.key = key;
            this.kind = kind;
        }

        public Object getKey() {
            return key;
        }

        public ElementKind getKind() {
            return kind;
        }
    }

    /** Builds the appropriate {@link ClassLiteralDescriptor} subclass for a class literal. */
    public static class Builder {
        private final ArrayList<MemberEntry> members = new ArrayList<>();
        private final ArrayList<Object> fieldKeys = new ArrayList<>();
        private int size = 1; // values[0] is always the constructor
        private boolean hasFieldInitializer;

        public void addMethod(Object key, ElementKind kind) {
            members.add(new MemberEntry(key, kind));
            size++;
        }

        /** Adds a method/getter/setter whose key value occupies the next {@code values} slot. */
        public void addComputedMethod(ElementKind kind) {
            members.add(new MemberEntry(UniqueTag.COMPUTED_KEY, kind));
            size += 2;
        }

        /**
         * Adds an instance field without an initializer. Fields must be added after all methods.
         */
        public void addField(Object key) {
            fieldKeys.add(key);
        }

        /**
         * Adds an instance field without an initializer whose key value occupies the next {@code
         * values} slot.
         */
        public void addComputedField() {
            fieldKeys.add(UniqueTag.COMPUTED_KEY);
            size++;
        }

        /** Adds the field initializer, which occupies the final {@code values} slot. */
        public void addFieldInitializer() {
            hasFieldInitializer = true;
            size++;
        }

        public int getSize() {
            return size;
        }

        public ClassLiteralDescriptor build() {
            if (!fieldKeys.isEmpty() || hasFieldInitializer) {
                return new FieldClassLiteralDescriptor(
                        members.toArray(new MemberEntry[0]),
                        fieldKeys.toArray(),
                        hasFieldInitializer);
            }
            if (members.isEmpty()) {
                return new SimpleClassLiteralDescriptor();
            }
            return new MethodClassLiteralDescriptor(members.toArray(new MemberEntry[0]));
        }
    }

    public static final class SimpleClassLiteralDescriptor extends ClassLiteralDescriptor {
        public static SimpleClassLiteralDescriptor simpleClassLiteralConstant(
                MethodHandles.Lookup lookup, String name, Class<?> type) {
            return new SimpleClassLiteralDescriptor();
        }

        @Override
        public Scriptable createClass(
                Context cx,
                VarScope scope,
                Object superClass,
                Scriptable prototype,
                Object[] values) {
            return setupClass((BaseFunction) values[0], prototype, superClass);
        }
    }

    public static final class MethodClassLiteralDescriptor extends ClassLiteralDescriptor {
        private final MemberEntry[] members;

        MethodClassLiteralDescriptor(MemberEntry[] members) {
            this.members = members;
        }

        public MemberEntry[] getMembers() {
            return members;
        }

        /** Bootstrap arguments are the key and kind of each member in turn. */
        public static MethodClassLiteralDescriptor methodClassLiteralConstant(
                MethodHandles.Lookup lookup, String name, Class<?> type, Object... keysAndKinds) {
            var members = new MemberEntry[keysAndKinds.length / 2];
            for (int i = 0; i < members.length; i++) {
                members[i] =
                        new MemberEntry(keysAndKinds[2 * i], (ElementKind) keysAndKinds[2 * i + 1]);
            }
            return new MethodClassLiteralDescriptor(members);
        }

        @Override
        public Scriptable createClass(
                Context cx,
                VarScope scope,
                Object superClass,
                Scriptable prototype,
                Object[] values) {
            BaseFunction constructor = setupClass((BaseFunction) values[0], prototype, superClass);
            installMembers((ScriptableObject) prototype, members, values);
            return constructor;
        }
    }

    /** A class with instance fields, and optionally methods. */
    public static final class FieldClassLiteralDescriptor extends ClassLiteralDescriptor {
        private final MemberEntry[] members;
        private final Object[] fieldKeys;
        private final boolean hasFieldInitializer;

        FieldClassLiteralDescriptor(
                MemberEntry[] members, Object[] fieldKeys, boolean hasFieldInitializer) {
            this.members = members;
            this.fieldKeys = fieldKeys;
            this.hasFieldInitializer = hasFieldInitializer;
        }

        public MemberEntry[] getMembers() {
            return members;
        }

        /** The keys of the fields without initializers. */
        public Object[] getFieldKeys() {
            return fieldKeys;
        }

        public boolean hasFieldInitializer() {
            return hasFieldInitializer;
        }

        /**
         * Bootstrap arguments are the number of members, the key and kind of each member in turn,
         * whether there is a field initializer, and then the key of each field without one.
         */
        public static FieldClassLiteralDescriptor fieldClassLiteralConstant(
                MethodHandles.Lookup lookup, String name, Class<?> type, Object... args) {
            int memberCount = (Integer) args[0];
            var members = new MemberEntry[memberCount];
            for (int i = 0; i < memberCount; i++) {
                members[i] = new MemberEntry(args[1 + 2 * i], (ElementKind) args[2 + 2 * i]);
            }
            int base = 1 + 2 * memberCount;
            boolean hasFieldInitializer = (Boolean) args[base];
            Object[] fieldKeys = Arrays.copyOfRange(args, base + 1, args.length);
            return new FieldClassLiteralDescriptor(members, fieldKeys, hasFieldInitializer);
        }

        @Override
        public Scriptable createClass(
                Context cx,
                VarScope scope,
                Object superClass,
                Scriptable prototype,
                Object[] values) {
            BaseFunction constructor = setupClass((BaseFunction) values[0], prototype, superClass);
            int v = installMembers((ScriptableObject) prototype, members, values);
            ((JSFunction) constructor).setFieldValues(Arrays.copyOfRange(values, v, values.length));
            return constructor;
        }

        @Override
        public Object initializeFields(
                Context cx, VarScope scope, Object constructor, Object thisObj) {
            if (!(constructor instanceof JSFunction) || !(thisObj instanceof Scriptable)) {
                return thisObj;
            }
            Object[] fieldValues = ((JSFunction) constructor).getFieldValues();
            if (fieldValues == null) {
                return thisObj;
            }
            Scriptable self = (Scriptable) thisObj;
            if (hasFieldInitializer) {
                Callable initializer = (Callable) fieldValues[fieldValues.length - 1];
                initializer.call(cx, scope, self, ScriptRuntime.emptyArgs);
            }
            int v = 0;
            for (Object key : fieldKeys) {
                if (key == UniqueTag.COMPUTED_KEY) {
                    key = fieldValues[v++];
                }
                defineField(key, Undefined.instance, cx, self);
            }
            return thisObj;
        }
    }
}
