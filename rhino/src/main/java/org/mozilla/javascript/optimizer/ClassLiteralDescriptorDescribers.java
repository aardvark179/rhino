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
}
