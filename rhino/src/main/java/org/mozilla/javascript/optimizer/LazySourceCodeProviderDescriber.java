/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.optimizer;

import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.DirectMethodHandleDesc;
import java.lang.constant.DynamicConstantDesc;
import org.mozilla.classfile.DynamicConstantDescriber;
import org.mozilla.classfile.DynamicDescriberRegistry;
import org.mozilla.javascript.LazySourceCodeProvider;

/**
 * Describes {@link LazySourceCodeProvider} as a dynamic constant. The supplier cannot be written
 * into a class file, so the resolved provider has none, and {@link Codegen} sets it once the class
 * has been defined. Every descriptor in a class shares the one provider the constant resolves to.
 */
public class LazySourceCodeProviderDescriber
        extends DynamicConstantDescriber<LazySourceCodeProvider> {

    private static final ClassDesc CD_LAZY_SOURCE_CODE_PROVIDER =
            ClassDesc.of(LazySourceCodeProvider.class.getName());

    private static final DirectMethodHandleDesc SOURCE_CONSTANT =
            ConstantDescs.ofConstantBootstrap(
                    CD_LAZY_SOURCE_CODE_PROVIDER, "sourceConstant", CD_LAZY_SOURCE_CODE_PROVIDER);

    public LazySourceCodeProviderDescriber(DynamicDescriberRegistry registry) {
        super(registry);
    }

    @Override
    public Class<LazySourceCodeProvider> describedType() {
        return LazySourceCodeProvider.class;
    }

    @Override
    public DynamicConstantDesc<LazySourceCodeProvider> describe(LazySourceCodeProvider value) {
        return DynamicConstantDesc.ofNamed(SOURCE_CONSTANT, "source", CD_LAZY_SOURCE_CODE_PROVIDER);
    }
}
