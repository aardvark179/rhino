package org.mozilla.javascript;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Runtime descriptor for an ES6 class literal, playing the same role for classes that {@link
 * ObjectLiteralDescriptor} plays for object literals: it is built once at parse time and describes
 * the class's shape, and is invoked identically from all three backends via a single {@link
 * #createClass} call.
 *
 * <p>{@code values[0]} is always the (already-built) constructor function. The rest of {@code
 * values} holds, per member entry in order: the computed key value (only for entries whose key is
 * {@link UniqueTag#COMPUTED_KEY}) followed by the member's function value. {@code prototype} is a
 * freshly created plain object (built the same way as an object literal's {@code EMPTY_OBJECT})
 * that becomes the constructor's {@code .prototype}; methods/the constructor are all built with
 * {@code homeObject == prototype} via the same "peek the object under construction" convention
 * object-literal methods already use.
 *
 * <p>Static members, fields, and private names will add further subclasses in later phases,
 * mirroring how {@link ObjectLiteralDescriptor} grew from a simple case to a complex one.
 */
public abstract class ClassLiteralDescriptor implements Serializable {

    public abstract Scriptable createClass(
            Context cx, VarScope scope, Object superClass, Scriptable prototype, Object[] values);

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

    private static final class MemberEntry implements Serializable {
        final Object key;
        final ElementKind kind;

        MemberEntry(Object key, ElementKind kind) {
            this.key = key;
            this.kind = kind;
        }
    }

    /** Builds the appropriate {@link ClassLiteralDescriptor} subclass for a class literal. */
    public static class Builder {
        private final ArrayList<MemberEntry> members = new ArrayList<>();
        private int size = 1; // values[0] is always the constructor

        public void addMethod(Object key, ElementKind kind) {
            members.add(new MemberEntry(key, kind));
            size++;
        }

        /** Adds a method/getter/setter whose key value occupies the next {@code values} slot. */
        public void addComputedMethod(ElementKind kind) {
            members.add(new MemberEntry(UniqueTag.COMPUTED_KEY, kind));
            size += 2;
        }

        public int getSize() {
            return size;
        }

        public ClassLiteralDescriptor build() {
            if (members.isEmpty()) {
                return new SimpleClassLiteralDescriptor();
            }
            return new MethodClassLiteralDescriptor(members.toArray(new MemberEntry[0]));
        }
    }

    private static final class SimpleClassLiteralDescriptor extends ClassLiteralDescriptor {
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

    private static final class MethodClassLiteralDescriptor extends ClassLiteralDescriptor {
        private final MemberEntry[] members;

        MethodClassLiteralDescriptor(MemberEntry[] members) {
            this.members = members;
        }

        @Override
        public Scriptable createClass(
                Context cx,
                VarScope scope,
                Object superClass,
                Scriptable prototype,
                Object[] values) {
            BaseFunction constructor = setupClass((BaseFunction) values[0], prototype, superClass);
            ScriptableObject target = (ScriptableObject) prototype;
            int v = 1;
            for (MemberEntry member : members) {
                Object key = member.key == UniqueTag.COMPUTED_KEY ? values[v++] : member.key;
                Object value = values[v++];
                installMember(target, key, member.kind, value);
            }
            return constructor;
        }
    }
}
