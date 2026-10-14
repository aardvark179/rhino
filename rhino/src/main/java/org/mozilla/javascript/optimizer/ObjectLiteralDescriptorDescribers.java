/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.optimizer;

import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.DirectMethodHandleDesc;
import java.lang.constant.DynamicConstantDesc;
import org.mozilla.classfile.DynamicConstantDescriber;
import org.mozilla.classfile.DynamicDescriberRegistry;
import org.mozilla.javascript.ObjectLiteralDescriptor.ComplexObjectLiteral;
import org.mozilla.javascript.ObjectLiteralDescriptor.ComputedKeyObjectLiteral;
import org.mozilla.javascript.ObjectLiteralDescriptor.GetterEntry;
import org.mozilla.javascript.ObjectLiteralDescriptor.SetterEntry;
import org.mozilla.javascript.ObjectLiteralDescriptor.SimpleClassLitDescriptor;
import org.mozilla.javascript.ObjectLiteralDescriptor.SpreadEntry;

/**
 * Describers for the {@link org.mozilla.javascript.ObjectLiteralDescriptor} subclasses, and the
 * entries that may appear in their ops. Keys and ops are passed as bootstrap arguments, with any
 * that are not already constants described through the registry.
 *
 * <p>Descriptors are immutable, so equal descriptors may safely share a constant.
 */
public final class ObjectLiteralDescriptorDescribers {

    private ObjectLiteralDescriptorDescribers() {}

    /** Register describers for all the object literal descriptor types with the registry. */
    public static void register(DynamicDescriberRegistry registry) {
        new SimpleDescriber(registry);
        new ComputedKeyDescriber(registry);
        new ComplexDescriber(registry);
        new GetterEntryDescriber(registry);
        new SetterEntryDescriber(registry);
        new SpreadEntryDescriber(registry);
    }

    private static ConstantDesc[] describeAll(DynamicDescriberRegistry registry, Object[] values) {
        var descs = new ConstantDesc[values.length];
        for (int i = 0; i < values.length; i++) {
            descs[i] = registry.describe(values[i]);
        }
        return descs;
    }

    private static DirectMethodHandleDesc arrayBootstrap(ClassDesc owner, String name) {
        return ConstantDescs.ofConstantBootstrap(
                owner, name, owner, ConstantDescs.CD_Object.arrayType());
    }

    private static DirectMethodHandleDesc keyBootstrap(ClassDesc owner, String name) {
        return ConstantDescs.ofConstantBootstrap(owner, name, owner, ConstantDescs.CD_Object);
    }

    public static class SimpleDescriber extends DynamicConstantDescriber<SimpleClassLitDescriptor> {

        private static final ClassDesc CD_SIMPLE =
                ClassDesc.of(SimpleClassLitDescriptor.class.getName());

        private static final DirectMethodHandleDesc SIMPLE_CONSTANT =
                arrayBootstrap(CD_SIMPLE, "simpleObjectLiteralConstant");

        public SimpleDescriber(DynamicDescriberRegistry registry) {
            super(registry);
        }

        @Override
        public Class<SimpleClassLitDescriptor> describedType() {
            return SimpleClassLitDescriptor.class;
        }

        @Override
        public DynamicConstantDesc<SimpleClassLitDescriptor> describe(
                SimpleClassLitDescriptor value) {
            return DynamicConstantDesc.ofNamed(
                    SIMPLE_CONSTANT,
                    "objectLiteral",
                    CD_SIMPLE,
                    describeAll(getRegistry(), value.getKeys()));
        }
    }

    public static class ComputedKeyDescriber
            extends DynamicConstantDescriber<ComputedKeyObjectLiteral> {

        private static final ClassDesc CD_COMPUTED_KEY =
                ClassDesc.of(ComputedKeyObjectLiteral.class.getName());

        private static final DirectMethodHandleDesc COMPUTED_KEY_CONSTANT =
                arrayBootstrap(CD_COMPUTED_KEY, "computedKeyObjectLiteralConstant");

        public ComputedKeyDescriber(DynamicDescriberRegistry registry) {
            super(registry);
        }

        @Override
        public Class<ComputedKeyObjectLiteral> describedType() {
            return ComputedKeyObjectLiteral.class;
        }

        @Override
        public DynamicConstantDesc<ComputedKeyObjectLiteral> describe(
                ComputedKeyObjectLiteral value) {
            return DynamicConstantDesc.ofNamed(
                    COMPUTED_KEY_CONSTANT,
                    "objectLiteral",
                    CD_COMPUTED_KEY,
                    describeAll(getRegistry(), value.getOps()));
        }
    }

    public static class ComplexDescriber extends DynamicConstantDescriber<ComplexObjectLiteral> {

        private static final ClassDesc CD_COMPLEX =
                ClassDesc.of(ComplexObjectLiteral.class.getName());

        private static final DirectMethodHandleDesc COMPLEX_CONSTANT =
                arrayBootstrap(CD_COMPLEX, "complexObjectLiteralConstant");

        public ComplexDescriber(DynamicDescriberRegistry registry) {
            super(registry);
        }

        @Override
        public Class<ComplexObjectLiteral> describedType() {
            return ComplexObjectLiteral.class;
        }

        @Override
        public DynamicConstantDesc<ComplexObjectLiteral> describe(ComplexObjectLiteral value) {
            return DynamicConstantDesc.ofNamed(
                    COMPLEX_CONSTANT,
                    "objectLiteral",
                    CD_COMPLEX,
                    describeAll(getRegistry(), value.getOps()));
        }
    }

    public static class GetterEntryDescriber extends DynamicConstantDescriber<GetterEntry> {

        private static final ClassDesc CD_GETTER = ClassDesc.of(GetterEntry.class.getName());

        private static final DirectMethodHandleDesc GETTER_CONSTANT =
                keyBootstrap(CD_GETTER, "getterEntryConstant");

        public GetterEntryDescriber(DynamicDescriberRegistry registry) {
            super(registry);
        }

        @Override
        public Class<GetterEntry> describedType() {
            return GetterEntry.class;
        }

        @Override
        public DynamicConstantDesc<GetterEntry> describe(GetterEntry value) {
            return DynamicConstantDesc.ofNamed(
                    GETTER_CONSTANT, "getter", CD_GETTER, getRegistry().describe(value.getKey()));
        }
    }

    public static class SetterEntryDescriber extends DynamicConstantDescriber<SetterEntry> {

        private static final ClassDesc CD_SETTER = ClassDesc.of(SetterEntry.class.getName());

        private static final DirectMethodHandleDesc SETTER_CONSTANT =
                keyBootstrap(CD_SETTER, "setterEntryConstant");

        public SetterEntryDescriber(DynamicDescriberRegistry registry) {
            super(registry);
        }

        @Override
        public Class<SetterEntry> describedType() {
            return SetterEntry.class;
        }

        @Override
        public DynamicConstantDesc<SetterEntry> describe(SetterEntry value) {
            return DynamicConstantDesc.ofNamed(
                    SETTER_CONSTANT, "setter", CD_SETTER, getRegistry().describe(value.getKey()));
        }
    }

    public static class SpreadEntryDescriber extends DynamicConstantDescriber<SpreadEntry> {

        private static final ClassDesc CD_SPREAD = ClassDesc.of(SpreadEntry.class.getName());

        private static final DirectMethodHandleDesc SPREAD_CONSTANT =
                ConstantDescs.ofConstantBootstrap(CD_SPREAD, "spreadEntryConstant", CD_SPREAD);

        public SpreadEntryDescriber(DynamicDescriberRegistry registry) {
            super(registry);
        }

        @Override
        public Class<SpreadEntry> describedType() {
            return SpreadEntry.class;
        }

        @Override
        public DynamicConstantDesc<SpreadEntry> describe(SpreadEntry value) {
            return DynamicConstantDesc.ofNamed(SPREAD_CONSTANT, "spread", CD_SPREAD);
        }
    }
}
