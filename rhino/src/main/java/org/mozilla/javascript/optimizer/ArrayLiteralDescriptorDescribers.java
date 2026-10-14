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
import org.mozilla.javascript.ArrayLiteralDescriptor.ComplexArrayLiteral;
import org.mozilla.javascript.ArrayLiteralDescriptor.SimpleArrayLiteral;

/**
 * Describers for the {@link org.mozilla.javascript.ArrayLiteralDescriptor} subclasses.
 *
 * <p>Descriptors are immutable, so equal descriptors may safely share a constant.
 */
public final class ArrayLiteralDescriptorDescribers {

    private ArrayLiteralDescriptorDescribers() {}

    /** Register describers for all the array literal descriptor types with the registry. */
    public static void register(DynamicDescriberRegistry registry) {
        new SimpleDescriber(registry);
        new ComplexDescriber(registry);
    }

    public static class SimpleDescriber extends DynamicConstantDescriber<SimpleArrayLiteral> {

        private static final ClassDesc CD_SIMPLE = ClassDesc.of(SimpleArrayLiteral.class.getName());

        private static final DirectMethodHandleDesc SIMPLE_CONSTANT =
                ConstantDescs.ofConstantBootstrap(
                        CD_SIMPLE, "simpleArrayLiteralConstant", CD_SIMPLE);

        public SimpleDescriber(DynamicDescriberRegistry registry) {
            super(registry);
        }

        @Override
        public Class<SimpleArrayLiteral> describedType() {
            return SimpleArrayLiteral.class;
        }

        @Override
        public DynamicConstantDesc<SimpleArrayLiteral> describe(SimpleArrayLiteral value) {
            return DynamicConstantDesc.ofNamed(SIMPLE_CONSTANT, "arrayLiteral", CD_SIMPLE);
        }
    }

    /**
     * The skip and spread positions are passed as a single list of bootstrap arguments, preceded by
     * the number of skips so that the two can be separated again.
     */
    public static class ComplexDescriber extends DynamicConstantDescriber<ComplexArrayLiteral> {

        private static final ClassDesc CD_COMPLEX =
                ClassDesc.of(ComplexArrayLiteral.class.getName());

        private static final DirectMethodHandleDesc COMPLEX_CONSTANT =
                ConstantDescs.ofConstantBootstrap(
                        CD_COMPLEX,
                        "complexArrayLiteralConstant",
                        CD_COMPLEX,
                        ConstantDescs.CD_int,
                        ConstantDescs.CD_int.arrayType());

        public ComplexDescriber(DynamicDescriberRegistry registry) {
            super(registry);
        }

        @Override
        public Class<ComplexArrayLiteral> describedType() {
            return ComplexArrayLiteral.class;
        }

        @Override
        public DynamicConstantDesc<ComplexArrayLiteral> describe(ComplexArrayLiteral value) {
            var skips = value.getSkips();
            var spreads = value.getSpreads();
            var descs = new ConstantDesc[1 + skips.length + spreads.length];
            descs[0] = Integer.valueOf(skips.length);
            for (int i = 0; i < skips.length; i++) {
                descs[1 + i] = Integer.valueOf(skips[i]);
            }
            for (int i = 0; i < spreads.length; i++) {
                descs[1 + skips.length + i] = Integer.valueOf(spreads[i]);
            }
            return DynamicConstantDesc.ofNamed(COMPLEX_CONSTANT, "arrayLiteral", CD_COMPLEX, descs);
        }
    }
}
