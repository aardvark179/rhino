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
import org.mozilla.javascript.NullSourceCodeProvider;

/**
 * Describes {@link NullSourceCodeProvider} as a dynamic constant resolving to the shared instance.
 */
public class NullSourceCodeProviderDescriber
        extends DynamicConstantDescriber<NullSourceCodeProvider> {

    private static final ClassDesc CD_NULL_SOURCE_CODE_PROVIDER =
            ClassDesc.of(NullSourceCodeProvider.class.getName());

    private static final DirectMethodHandleDesc SOURCE_CONSTANT =
            ConstantDescs.ofConstantBootstrap(
                    CD_NULL_SOURCE_CODE_PROVIDER, "sourceConstant", CD_NULL_SOURCE_CODE_PROVIDER);

    public NullSourceCodeProviderDescriber(DynamicDescriberRegistry registry) {
        super(registry);
    }

    @Override
    public Class<NullSourceCodeProvider> describedType() {
        return NullSourceCodeProvider.class;
    }

    @Override
    public DynamicConstantDesc<NullSourceCodeProvider> describe(NullSourceCodeProvider value) {
        return DynamicConstantDesc.ofNamed(SOURCE_CONSTANT, "source", CD_NULL_SOURCE_CODE_PROVIDER);
    }
}
