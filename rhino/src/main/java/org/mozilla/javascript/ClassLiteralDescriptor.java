package org.mozilla.javascript;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.IntFunction;

/**
 * Runtime descriptor for an ES6 class literal, playing the same role for classes that {@link
 * ObjectLiteralDescriptor} plays for object literals: it is built once at parse time and describes
 * the class's shape, and is invoked identically from all three backends via a single {@link
 * #createClass} call.
 *
 * <p>{@code values[0]} is always the (already-built) constructor function. The rest of {@code
 * values} holds the computed key value of each member entry whose key is {@link
 * UniqueTag#COMPUTED_KEY}, in order. The members' functions are not among the values: {@link
 * #createClass} creates them from their {@link JSDescriptor}s, with the prototype, or for static
 * members the constructor, as their home object. A member's descriptor is either held by its entry
 * or, if not, found by its function index among the nested functions of the {@code parent}
 * descriptor passed to {@link #createClass}. After the members come the computed key values of any
 * instance fields with computed keys, in declaration order, and, if any field has an initializer, a
 * single field initializer function, which creates the fields with initializers on its {@code this}
 * in declaration order. The field initializer is passed the computed keys of the fields with
 * initializers as its arguments. The static fields' computed key values and static field
 * initializer follow in the same way. {@code prototype} is a freshly created plain object (built
 * the same way as an object literal's {@code EMPTY_OBJECT}) that becomes the constructor's {@code
 * .prototype}; the constructor and field initializers are built with {@code homeObject ==
 * prototype} via the same "peek the object under construction" convention object-literal methods
 * already use.
 *
 * <p>Static fields are defined on the constructor by {@link #createClass}, once its members are
 * installed: first those with initializers, by calling the static field initializer with the
 * constructor as {@code this}, and then the rest, as {@code undefined}.
 *
 * <p>Instance fields are not installed by {@link #createClass}: the field initializer and the keys
 * of the fields without initializers are recorded on the constructor, and {@link #initializeFields}
 * defines them on each new instance once its {@code this} value is available - at the start of a
 * base class constructor, or after the {@code super(...)} call of a derived one.
 */
public abstract class ClassLiteralDescriptor implements Serializable {

    public abstract Scriptable createClass(
            Context cx,
            VarScope scope,
            JSDescriptor<?> parent,
            Object superClass,
            Scriptable prototype,
            Object[] values);

    /**
     * Returns a copy of this descriptor whose members hold their function descriptors, as given for
     * each function index by {@code functions}, so that {@link #createClass} does not need a parent
     * descriptor to find them. Used by compiled code, where the values are {@link
     * JSDescriptor.Builder}s to be described as constants, which resolve to {@link JSDescriptor}s.
     */
    public ClassLiteralDescriptor withMemberFunctions(IntFunction<Object> functions) {
        return this;
    }

    /**
     * Initializes the instance fields of class constructor {@code constructor} on {@code thisObj},
     * and returns {@code thisObj}: first the fields with initializers, by calling the field
     * initializer with the computed keys of those fields, and then the rest, as {@code undefined}.
     * The computed keys and field initializer captured when the class was evaluated are held by the
     * constructor.
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

    /**
     * Creates the functions of {@code members} in {@code scope} and installs them on {@code
     * prototype}, or, for static members, on {@code constructor}, which is also their home object
     * in place of {@code prototype}. Returns the index of the next unused value.
     */
    @SuppressWarnings("unchecked")
    private static int installMembers(
            Context cx,
            VarScope scope,
            JSDescriptor<?> parent,
            ScriptableObject prototype,
            BaseFunction constructor,
            MemberEntry[] members,
            Object[] values) {
        int v = 1;
        for (MemberEntry member : members) {
            Object key = member.key == UniqueTag.COMPUTED_KEY ? values[v++] : member.key;
            JSDescriptor<JSFunction> desc =
                    member.function != null
                            ? (JSDescriptor<JSFunction>) member.function
                            : parent.getFunction(member.functionIndex);
            ScriptableObject target = member.isStatic ? constructor : prototype;
            JSFunction fn = new JSFunction(cx, scope, desc, null, Undefined.instance, target);
            installMember(target, key, member.kind, fn);
        }
        return v;
    }

    private static MemberEntry[] withMemberFunctions(
            MemberEntry[] members, IntFunction<Object> functions) {
        var result = new MemberEntry[members.length];
        for (int i = 0; i < members.length; i++) {
            MemberEntry m = members[i];
            result[i] =
                    new MemberEntry(
                            m.key,
                            m.kind,
                            m.isStatic,
                            m.functionIndex,
                            functions.apply(m.functionIndex));
        }
        return result;
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

    /** The number of bootstrap arguments describing each member. */
    public static final int MEMBER_ARG_COUNT = 5;

    /**
     * Reads {@code count} members, each as key, kind, staticness, function index, and function
     * descriptor (or {@code null}), from {@code args[start]}.
     */
    private static MemberEntry[] readMembers(Object[] args, int start, int count) {
        var members = new MemberEntry[count];
        for (int i = 0; i < count; i++) {
            int a = start + MEMBER_ARG_COUNT * i;
            members[i] =
                    new MemberEntry(
                            args[a],
                            (ElementKind) args[a + 1],
                            (Boolean) args[a + 2],
                            (Integer) args[a + 3],
                            args[a + 4]);
        }
        return members;
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
        final boolean isStatic;
        final int functionIndex;
        final Object function;

        MemberEntry(
                Object key,
                ElementKind kind,
                boolean isStatic,
                int functionIndex,
                Object function) {
            this.key = key;
            this.kind = kind;
            this.isStatic = isStatic;
            this.functionIndex = functionIndex;
            this.function = function;
        }

        public Object getKey() {
            return key;
        }

        public ElementKind getKind() {
            return kind;
        }

        public boolean isStatic() {
            return isStatic;
        }

        /** The index of this member's function among the nested functions of its parent. */
        public int getFunctionIndex() {
            return functionIndex;
        }

        /** This member's function descriptor, or {@code null} if it must be found by index. */
        public Object getFunction() {
            return function;
        }
    }

    /** Builds the appropriate {@link ClassLiteralDescriptor} subclass for a class literal. */
    public static class Builder {
        private final ArrayList<MemberEntry> members = new ArrayList<>();
        private final ArrayList<Object> fieldKeys = new ArrayList<>();
        private final ArrayList<Object> staticFieldKeys = new ArrayList<>();
        private int size = 1; // values[0] is always the constructor
        private boolean hasFieldInitializer;
        private boolean hasStaticFieldInitializer;

        /**
         * Adds a (static) method/getter/setter whose function is the parent's nested function
         * {@code functionIndex}.
         */
        public void addMethod(Object key, ElementKind kind, boolean isStatic, int functionIndex) {
            members.add(new MemberEntry(key, kind, isStatic, functionIndex, null));
        }

        /**
         * Adds a (static) method/getter/setter whose key value occupies the next {@code values}
         * slot, and whose function is the parent's nested function {@code functionIndex}.
         */
        public void addComputedMethod(ElementKind kind, boolean isStatic, int functionIndex) {
            members.add(
                    new MemberEntry(UniqueTag.COMPUTED_KEY, kind, isStatic, functionIndex, null));
            size++;
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

        /**
         * Adds an instance field with an initializer whose key value occupies the next {@code
         * values} slot. Fields must be added after all methods.
         */
        public void addComputedInitializedField() {
            fieldKeys.add(UniqueTag.COMPUTED_INITIALIZED_FIELD);
            size++;
        }

        /** Adds the field initializer, which occupies the next {@code values} slot. */
        public void addFieldInitializer() {
            hasFieldInitializer = true;
            size++;
        }

        /**
         * Adds a static field without an initializer. Static fields must be added after all
         * instance fields.
         */
        public void addStaticField(Object key) {
            staticFieldKeys.add(key);
        }

        /**
         * Adds a static field without an initializer whose key value occupies the next {@code
         * values} slot.
         */
        public void addComputedStaticField() {
            staticFieldKeys.add(UniqueTag.COMPUTED_KEY);
            size++;
        }

        /**
         * Adds a static field with an initializer whose key value occupies the next {@code values}
         * slot.
         */
        public void addComputedInitializedStaticField() {
            staticFieldKeys.add(UniqueTag.COMPUTED_INITIALIZED_FIELD);
            size++;
        }

        /** Adds the static field initializer, which occupies the final {@code values} slot. */
        public void addStaticFieldInitializer() {
            hasStaticFieldInitializer = true;
            size++;
        }

        public int getSize() {
            return size;
        }

        public ClassLiteralDescriptor build() {
            if (!fieldKeys.isEmpty()
                    || hasFieldInitializer
                    || !staticFieldKeys.isEmpty()
                    || hasStaticFieldInitializer) {
                return new FieldClassLiteralDescriptor(
                        members.toArray(new MemberEntry[0]),
                        fieldKeys.toArray(),
                        hasFieldInitializer,
                        staticFieldKeys.toArray(),
                        hasStaticFieldInitializer);
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
                JSDescriptor<?> parent,
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

        /**
         * Bootstrap arguments are the key, kind, staticness, function index, and function
         * descriptor of each member in turn.
         */
        public static MethodClassLiteralDescriptor methodClassLiteralConstant(
                MethodHandles.Lookup lookup, String name, Class<?> type, Object... args) {
            return new MethodClassLiteralDescriptor(
                    readMembers(args, 0, args.length / MEMBER_ARG_COUNT));
        }

        @Override
        public ClassLiteralDescriptor withMemberFunctions(IntFunction<Object> functions) {
            return new MethodClassLiteralDescriptor(
                    ClassLiteralDescriptor.withMemberFunctions(members, functions));
        }

        @Override
        public Scriptable createClass(
                Context cx,
                VarScope scope,
                JSDescriptor<?> parent,
                Object superClass,
                Scriptable prototype,
                Object[] values) {
            BaseFunction constructor = setupClass((BaseFunction) values[0], prototype, superClass);
            installMembers(
                    cx, scope, parent, (ScriptableObject) prototype, constructor, members, values);
            return constructor;
        }
    }

    /** A class with instance and/or static fields, and optionally methods. */
    public static final class FieldClassLiteralDescriptor extends ClassLiteralDescriptor {
        private final MemberEntry[] members;
        private final Object[] fieldKeys;
        private final boolean hasFieldInitializer;
        private final Object[] staticFieldKeys;
        private final boolean hasStaticFieldInitializer;
        private final int fieldValueCount;

        FieldClassLiteralDescriptor(
                MemberEntry[] members,
                Object[] fieldKeys,
                boolean hasFieldInitializer,
                Object[] staticFieldKeys,
                boolean hasStaticFieldInitializer) {
            this.members = members;
            this.fieldKeys = fieldKeys;
            this.hasFieldInitializer = hasFieldInitializer;
            this.staticFieldKeys = staticFieldKeys;
            this.hasStaticFieldInitializer = hasStaticFieldInitializer;
            this.fieldValueCount = valueCount(fieldKeys, hasFieldInitializer);
        }

        public MemberEntry[] getMembers() {
            return members;
        }

        /**
         * The keys of the fields without initializers, with {@link
         * UniqueTag#COMPUTED_INITIALIZED_FIELD} marking the position of each computed key of a
         * field with an initializer.
         */
        public Object[] getFieldKeys() {
            return fieldKeys;
        }

        public boolean hasFieldInitializer() {
            return hasFieldInitializer;
        }

        /** The keys of the static fields, in the same form as {@link #getFieldKeys}. */
        public Object[] getStaticFieldKeys() {
            return staticFieldKeys;
        }

        public boolean hasStaticFieldInitializer() {
            return hasStaticFieldInitializer;
        }

        /**
         * Bootstrap arguments are the number of members, the key, kind, staticness, function index,
         * and function descriptor of each member in turn, whether there is a field initializer, the
         * number of field keys, the field keys, whether there is a static field initializer, and
         * then the static field keys.
         */
        public static FieldClassLiteralDescriptor fieldClassLiteralConstant(
                MethodHandles.Lookup lookup, String name, Class<?> type, Object... args) {
            int memberCount = (Integer) args[0];
            var members = readMembers(args, 1, memberCount);
            int base = 1 + MEMBER_ARG_COUNT * memberCount;
            boolean hasFieldInitializer = (Boolean) args[base];
            int fieldKeyCount = (Integer) args[base + 1];
            int fieldKeysEnd = base + 2 + fieldKeyCount;
            Object[] fieldKeys = Arrays.copyOfRange(args, base + 2, fieldKeysEnd);
            boolean hasStaticFieldInitializer = (Boolean) args[fieldKeysEnd];
            Object[] staticFieldKeys = Arrays.copyOfRange(args, fieldKeysEnd + 1, args.length);
            return new FieldClassLiteralDescriptor(
                    members,
                    fieldKeys,
                    hasFieldInitializer,
                    staticFieldKeys,
                    hasStaticFieldInitializer);
        }

        @Override
        public ClassLiteralDescriptor withMemberFunctions(IntFunction<Object> functions) {
            return new FieldClassLiteralDescriptor(
                    ClassLiteralDescriptor.withMemberFunctions(members, functions),
                    fieldKeys,
                    hasFieldInitializer,
                    staticFieldKeys,
                    hasStaticFieldInitializer);
        }

        @Override
        public Scriptable createClass(
                Context cx,
                VarScope scope,
                JSDescriptor<?> parent,
                Object superClass,
                Scriptable prototype,
                Object[] values) {
            BaseFunction constructor = setupClass((BaseFunction) values[0], prototype, superClass);
            int v =
                    installMembers(
                            cx,
                            scope,
                            parent,
                            (ScriptableObject) prototype,
                            constructor,
                            members,
                            values);
            ((JSFunction) constructor)
                    .setFieldValues(Arrays.copyOfRange(values, v, v + fieldValueCount));
            v += fieldValueCount;
            defineFields(
                    cx, scope, constructor, staticFieldKeys, hasStaticFieldInitializer, values, v);
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
            defineFields(
                    cx,
                    scope,
                    (Scriptable) thisObj,
                    fieldKeys,
                    hasFieldInitializer,
                    fieldValues,
                    0);
            return thisObj;
        }

        /** The number of {@code values} slots occupied by fields with {@code keys}. */
        private static int valueCount(Object[] keys, boolean hasInitializer) {
            int count = hasInitializer ? 1 : 0;
            for (Object key : keys) {
                if (key == UniqueTag.COMPUTED_KEY || key == UniqueTag.COMPUTED_INITIALIZED_FIELD) {
                    count++;
                }
            }
            return count;
        }

        /**
         * Defines the fields with {@code keys} on {@code target}: first those with initializers, by
         * calling the initializer with their computed keys, and then the rest, as {@code
         * undefined}. The computed keys, followed by the initializer if any, start at {@code
         * values[start]}.
         */
        private static void defineFields(
                Context cx,
                VarScope scope,
                Scriptable target,
                Object[] keys,
                boolean hasInitializer,
                Object[] values,
                int start) {
            if (hasInitializer) {
                ArrayList<Object> initializerKeys = new ArrayList<>();
                int v = start;
                for (Object key : keys) {
                    if (key == UniqueTag.COMPUTED_INITIALIZED_FIELD) {
                        initializerKeys.add(values[v++]);
                    } else if (key == UniqueTag.COMPUTED_KEY) {
                        v++;
                    }
                }
                Callable initializer = (Callable) values[v];
                initializer.call(cx, scope, target, initializerKeys.toArray());
            }
            int v = start;
            for (Object key : keys) {
                if (key == UniqueTag.COMPUTED_INITIALIZED_FIELD) {
                    v++;
                    continue;
                }
                if (key == UniqueTag.COMPUTED_KEY) {
                    key = values[v++];
                }
                defineField(key, Undefined.instance, cx, target);
            }
        }
    }
}
