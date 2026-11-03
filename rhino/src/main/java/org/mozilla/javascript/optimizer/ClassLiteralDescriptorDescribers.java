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
import org.mozilla.javascript.ClassLiteralDescriptor.FieldClassLiteralDescriptor;
import org.mozilla.javascript.ClassLiteralDescriptor.MethodClassLiteralDescriptor;
import org.mozilla.javascript.ClassLiteralDescriptor.SimpleClassLiteralDescriptor;

/**
 * Describers for the {@link org.mozilla.javascript.ClassLiteralDescriptor} subclasses.
 *
 * <p>Descriptors are immutable, so equal descriptors may safely share a constant.
 */
public final class ClassLiteralDescriptorDescribers {

    private ClassLiteralDescriptorDescribers() {}

    /** Register describers for all the class literal descriptor types with the registry. */
    public static void register(DynamicDescriberRegistry registry) {
        new SimpleDescriber(registry);
        new MethodDescriber(registry);
        new FieldDescriber(registry);
    }

    public static class SimpleDescriber
            extends DynamicConstantDescriber<SimpleClassLiteralDescriptor> {

        private static final ClassDesc CD_SIMPLE =
                ClassDesc.of(SimpleClassLiteralDescriptor.class.getName());

        private static final DirectMethodHandleDesc SIMPLE_CONSTANT =
                ConstantDescs.ofConstantBootstrap(
                        CD_SIMPLE, "simpleClassLiteralConstant", CD_SIMPLE);

        public SimpleDescriber(DynamicDescriberRegistry registry) {
            super(registry);
        }

        @Override
        public Class<SimpleClassLiteralDescriptor> describedType() {
            return SimpleClassLiteralDescriptor.class;
        }

        @Override
        public DynamicConstantDesc<SimpleClassLiteralDescriptor> describe(
                SimpleClassLiteralDescriptor value) {
            return DynamicConstantDesc.ofNamed(SIMPLE_CONSTANT, "classLiteral", CD_SIMPLE);
        }
    }

    /**
     * Each member is passed as two bootstrap arguments: its key, described through the registry,
     * followed by its kind as an enum constant.
     */
    public static class MethodDescriber
            extends DynamicConstantDescriber<MethodClassLiteralDescriptor> {

        private static final ClassDesc CD_METHOD =
                ClassDesc.of(MethodClassLiteralDescriptor.class.getName());

        private static final DirectMethodHandleDesc METHOD_CONSTANT =
                ConstantDescs.ofConstantBootstrap(
                        CD_METHOD,
                        "methodClassLiteralConstant",
                        CD_METHOD,
                        ConstantDescs.CD_Object.arrayType());

        public MethodDescriber(DynamicDescriberRegistry registry) {
            super(registry);
        }

        @Override
        public Class<MethodClassLiteralDescriptor> describedType() {
            return MethodClassLiteralDescriptor.class;
        }

        @Override
        public DynamicConstantDesc<MethodClassLiteralDescriptor> describe(
                MethodClassLiteralDescriptor value) {
            var members = value.getMembers();
            var descs = new ConstantDesc[members.length * 2];
            for (int i = 0; i < members.length; i++) {
                descs[2 * i] = getRegistry().describe(members[i].getKey());
                descs[2 * i + 1] = members[i].getKind().describeConstable().orElseThrow();
            }
            return DynamicConstantDesc.ofNamed(METHOD_CONSTANT, "classLiteral", CD_METHOD, descs);
        }
    }

    /**
     * Bootstrap arguments are the number of members, then each member's key and kind as for {@link
     * MethodDescriber}, whether there is a field initializer, and finally the key of each field
     * without an initializer, all keys being described through the registry.
     */
    public static class FieldDescriber
            extends DynamicConstantDescriber<FieldClassLiteralDescriptor> {

        private static final ClassDesc CD_FIELD =
                ClassDesc.of(FieldClassLiteralDescriptor.class.getName());

        private static final DirectMethodHandleDesc FIELD_CONSTANT =
                ConstantDescs.ofConstantBootstrap(
                        CD_FIELD,
                        "fieldClassLiteralConstant",
                        CD_FIELD,
                        ConstantDescs.CD_Object.arrayType());

        public FieldDescriber(DynamicDescriberRegistry registry) {
            super(registry);
        }

        @Override
        public Class<FieldClassLiteralDescriptor> describedType() {
            return FieldClassLiteralDescriptor.class;
        }

        @Override
        public DynamicConstantDesc<FieldClassLiteralDescriptor> describe(
                FieldClassLiteralDescriptor value) {
            var members = value.getMembers();
            var fieldKeys = value.getFieldKeys();
            var descs = new ConstantDesc[2 + members.length * 2 + fieldKeys.length];
            descs[0] = members.length;
            for (int i = 0; i < members.length; i++) {
                descs[1 + 2 * i] = getRegistry().describe(members[i].getKey());
                descs[2 + 2 * i] = members[i].getKind().describeConstable().orElseThrow();
            }
            int base = 1 + 2 * members.length;
            descs[base] = value.hasFieldInitializer() ? ConstantDescs.TRUE : ConstantDescs.FALSE;
            for (int i = 0; i < fieldKeys.length; i++) {
                descs[base + 1 + i] = getRegistry().describe(fieldKeys[i]);
            }
            return DynamicConstantDesc.ofNamed(FIELD_CONSTANT, "classLiteral", CD_FIELD, descs);
        }
    }
}
