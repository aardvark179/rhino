package org.mozilla.javascript;

import java.io.Serializable;

/**
 * Runtime descriptor for an ES6 class literal, playing the same role for classes that {@link
 * ObjectLiteralDescriptor} plays for object literals: it is built once at parse time and describes
 * the class's shape, and is invoked identically from all three backends via a single {@link
 * #createClass} call.
 *
 * <p>This is a phase-1, minimal shape: a class is only a (possibly synthesized) constructor and an
 * optional {@code extends} clause. There is only one concrete subclass for now; methods,
 * getters/setters, fields, and private names will add further subclasses (and a non-empty {@code
 * values} array) in later phases, mirroring how {@link ObjectLiteralDescriptor} grew from a simple
 * case to a complex one.
 */
public abstract class ClassLiteralDescriptor implements Serializable {

    public abstract Scriptable createClass(
            Context cx,
            VarScope scope,
            Object superClass,
            BaseFunction constructor,
            Object[] values);

    /**
     * Wires up the constructor/prototype relationship described in ES2015 15.7.14
     * ClassDefinitionEvaluation, for the constructor's own parent and its {@code .prototype}'s
     * parent. (The non-enumerable {@code prototype.constructor} back-reference is already set up by
     * ordinary closure creation, same as for any other function's default prototype.)
     */
    static Scriptable setupClass(BaseFunction constructor, Object superClass) {
        if (Undefined.isUndefined(superClass)) {
            return constructor;
        }

        Scriptable superProto;
        if (superClass == null) {
            superProto = null;
        } else if (superClass instanceof Constructable && superClass instanceof Scriptable) {
            Scriptable superClassObj = (Scriptable) superClass;
            Object proto = superClassObj.get("prototype", superClassObj);
            if (proto == Scriptable.NOT_FOUND || Undefined.isUndefined(proto)) {
                superProto = null;
            } else if (proto instanceof Scriptable) {
                superProto = (Scriptable) proto;
            } else {
                throw ScriptRuntime.typeErrorById("msg.extends.not.ctor");
            }
            constructor.setPrototype(superClassObj);
        } else {
            throw ScriptRuntime.typeErrorById("msg.extends.not.ctor");
        }

        Object protoProperty = constructor.getPrototypeProperty();
        if (protoProperty instanceof ScriptableObject) {
            ((ScriptableObject) protoProperty).setPrototype(superProto);
        }
        return constructor;
    }

    /** Builds the appropriate {@link ClassLiteralDescriptor} subclass for a class literal. */
    public static class Builder {
        public ClassLiteralDescriptor build() {
            return new SimpleClassLiteralDescriptor();
        }
    }

    private static final class SimpleClassLiteralDescriptor extends ClassLiteralDescriptor {
        @Override
        public Scriptable createClass(
                Context cx,
                VarScope scope,
                Object superClass,
                BaseFunction constructor,
                Object[] values) {
            return setupClass(constructor, superClass);
        }
    }
}
