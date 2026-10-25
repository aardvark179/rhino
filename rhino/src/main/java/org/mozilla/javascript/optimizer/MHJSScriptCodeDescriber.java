/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.optimizer;

import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.DirectMethodHandleDesc;
import java.lang.constant.DirectMethodHandleDesc.Kind;
import java.lang.constant.DynamicConstantDesc;
import java.lang.constant.MethodHandleDesc;
import org.mozilla.classfile.DynamicConstantDescriber;
import org.mozilla.classfile.DynamicDescriberRegistry;
import org.mozilla.javascript.EagerSourceCodeProvider;

/**
 * Describes {@link EagerSourceCodeProvider} as a dynamic constant that is reconstructed from its
 * raw source when the constant is first resolved.
 *
 * <p>The source cannot be carried in the constant's name, because a name may not be empty and may
 * not contain the "[" and ";" characters that ordinary JavaScript source is full of. The source is
 * therefore a bootstrap argument, and the name is a fixed placeholder.
 */
public class MHJSScriptCodeDescriber extends DynamicConstantDescriber<MHJSScriptCode.Builder> {

    private static final ClassDesc CD_MH_JS_FUUCTION_CDDE =
            ClassDesc.of(MHJSScriptCode.class.getName());

    private static final DirectMethodHandleDesc CODE_CONSTANT =
            ConstantDescs.ofConstantBootstrap(
                    CD_MH_JS_FUUCTION_CDDE,
                    "bootstrap",
                    CD_MH_JS_FUUCTION_CDDE,
                    ConstantDescs.CD_MethodHandle,
                    ConstantDescs.CD_MethodHandle);

    private static final DirectMethodHandleDesc CODE_NO_RESUME_CONSTANT =
            ConstantDescs.ofConstantBootstrap(
                    CD_MH_JS_FUUCTION_CDDE,
                    "bootstrap",
                    CD_MH_JS_FUUCTION_CDDE,
                    ConstantDescs.CD_MethodHandle);

    public MHJSScriptCodeDescriber(DynamicDescriberRegistry registry) {
        super(registry);
    }

    @Override
    public Class<MHJSScriptCode.Builder> describedType() {
        return MHJSScriptCode.Builder.class;
    }

    @Override
    public DynamicConstantDesc<MHJSScriptCode.Builder> describe(MHJSScriptCode.Builder value) {
        var execMHD =
                MethodHandleDesc.of(
                        Kind.STATIC,
                        ClassDesc.of(value.env.className),
                        value.methodName,
                        value.methodType);
        return DynamicConstantDesc.ofNamed(
                CODE_NO_RESUME_CONSTANT, "functionCode", CD_MH_JS_FUUCTION_CDDE, execMHD);
    }
}
