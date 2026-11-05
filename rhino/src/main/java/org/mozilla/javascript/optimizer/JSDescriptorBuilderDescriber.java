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
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import org.mozilla.classfile.DynamicConstantDescriber;
import org.mozilla.classfile.DynamicDescriberRegistry;
import org.mozilla.javascript.JSCode;
import org.mozilla.javascript.JSDescriptor;
import org.mozilla.javascript.SourceCodeProvider;

/**
 * Describes a {@link JSDescriptor.Builder} as a dynamic constant that resolves to the {@link
 * JSDescriptor} it would build. The code, constructor, parent, and source code provider are
 * described through the registry, so describers for their types must also be registered.
 *
 * <p>The boolean properties are packed into a single flags argument, and the arrays are flattened
 * into the bootstrap arguments, each preceded by its length. The function statement names come last
 * and take up the remaining arguments.
 *
 * <p>Nested functions are not included, as compiled code does not need them, and the security
 * controller and domain are left null, as they are for compiled descriptors.
 */
@SuppressWarnings("rawtypes")
public class JSDescriptorBuilderDescriber extends DynamicConstantDescriber<JSDescriptor.Builder> {

    private static final ClassDesc CD_JS_DESCRIPTOR = ClassDesc.of(JSDescriptor.class.getName());
    private static final ClassDesc CD_JS_CODE = ClassDesc.of(JSCode.class.getName());
    private static final ClassDesc CD_SOURCE_PROVIDER =
            ClassDesc.of(SourceCodeProvider.class.getName());

    private static final DirectMethodHandleDesc DESCRIPTOR_CONSTANT =
            ConstantDescs.ofConstantBootstrap(
                    ClassDesc.of(JSDescriptorBuilderDescriber.class.getName()),
                    "descriptorConstant",
                    CD_JS_DESCRIPTOR,
                    CD_JS_CODE,
                    CD_JS_CODE,
                    CD_JS_DESCRIPTOR,
                    ConstantDescs.CD_int,
                    ConstantDescs.CD_String,
                    CD_SOURCE_PROVIDER,
                    ConstantDescs.CD_int,
                    ConstantDescs.CD_int,
                    ConstantDescs.CD_String,
                    ConstantDescs.CD_int,
                    ConstantDescs.CD_int,
                    ConstantDescs.CD_int,
                    ConstantDescs.CD_int,
                    ConstantDescs.CD_int,
                    ConstantDescs.CD_Object.arrayType());

    private static final int IS_STRICT_FLAG = 1;
    private static final int IS_SCRIPT_FLAG = 1 << 1;
    private static final int IS_TOP_LEVEL_FLAG = 1 << 2;
    private static final int IS_ES6_GENERATOR_FLAG = 1 << 3;
    private static final int IS_SHORTHAND_FLAG = 1 << 4;
    private static final int HAS_PROTOTYPE_FLAG = 1 << 5;
    private static final int HAS_LEXICAL_THIS_FLAG = 1 << 6;
    private static final int IS_EVAL_FUNCTION_FLAG = 1 << 7;
    private static final int HAS_REST_ARG_FLAG = 1 << 8;
    private static final int HAS_DEFAULT_PARAMETERS_FLAG = 1 << 9;
    private static final int REQUIRES_ACTIVATION_FRAME_FLAG = 1 << 10;
    private static final int REQUIRES_ARGUMENT_OBJECT_FLAG = 1 << 11;
    private static final int DECLARED_AS_FUNCTION_EXPRESSION_FLAG = 1 << 12;
    private static final int IS_ASYNC_FLAG = 1 << 13;
    private static final int DERIVED_CONSTRUCTOR_FLAG = 1 << 14;

    public JSDescriptorBuilderDescriber(DynamicDescriberRegistry registry) {
        super(registry);
    }

    @Override
    public Class<JSDescriptor.Builder> describedType() {
        return JSDescriptor.Builder.class;
    }

    @Override
    public DynamicConstantDesc<JSDescriptor.Builder> describe(JSDescriptor.Builder value) {
        var args = new ArrayList<ConstantDesc>();
        args.add(describeCode(value.code));
        args.add(describeCode(value.constructor));
        args.add(describeOrNull(value.parent));
        args.add(Integer.valueOf(flags(value)));
        args.add(describeOrNull(value.sourceFile));
        args.add(describeOrNull(value.sourceCodeProvider));
        args.add(Integer.valueOf(value.sourceStart));
        args.add(Integer.valueOf(value.sourceEnd));
        args.add(describeOrNull(value.name));
        args.add(Integer.valueOf(value.languageVersion));
        args.add(Integer.valueOf(value.paramAndVarCount));
        args.add(Integer.valueOf(value.paramCount));
        args.add(Integer.valueOf(value.arity));
        args.add(Integer.valueOf(value.functionType));
        args.add(Integer.valueOf(value.paramAndVarNames.length));
        for (String paramName : value.paramAndVarNames) {
            args.add(paramName);
        }
        args.add(Integer.valueOf(value.paramIsConst.length));
        for (boolean isConst : value.paramIsConst) {
            args.add(Integer.valueOf(isConst ? 1 : 0));
        }
        for (String statementName : value.functionStatementNames()) {
            args.add(statementName);
        }
        return DynamicConstantDesc.ofNamed(
                DESCRIPTOR_CONSTANT,
                "descriptor",
                CD_JS_DESCRIPTOR,
                args.toArray(new ConstantDesc[0]));
    }

    private ConstantDesc describeCode(JSCode.Builder<?> code) {
        return code instanceof JSCode.NullBuilder<?> ? ConstantDescs.NULL : describeOrNull(code);
    }

    private ConstantDesc describeOrNull(Object value) {
        return value == null ? ConstantDescs.NULL : getRegistry().describe(value);
    }

    private static int flags(JSDescriptor.Builder<?> value) {
        int flags = 0;
        flags |= value.isStrict ? IS_STRICT_FLAG : 0;
        flags |= value.isScript ? IS_SCRIPT_FLAG : 0;
        flags |= value.isTopLevel ? IS_TOP_LEVEL_FLAG : 0;
        flags |= value.isES6Generator ? IS_ES6_GENERATOR_FLAG : 0;
        flags |= value.isShorthand ? IS_SHORTHAND_FLAG : 0;
        flags |= value.hasPrototype ? HAS_PROTOTYPE_FLAG : 0;
        flags |= value.hasLexicalThis ? HAS_LEXICAL_THIS_FLAG : 0;
        flags |= value.isEvalFunction ? IS_EVAL_FUNCTION_FLAG : 0;
        flags |= value.hasRestArg ? HAS_REST_ARG_FLAG : 0;
        flags |= value.hasDefaultParameters ? HAS_DEFAULT_PARAMETERS_FLAG : 0;
        flags |= value.requiresActivationFrame ? REQUIRES_ACTIVATION_FRAME_FLAG : 0;
        flags |= value.requiresArgumentObject ? REQUIRES_ARGUMENT_OBJECT_FLAG : 0;
        flags |= value.declaredAsFunctionExpression ? DECLARED_AS_FUNCTION_EXPRESSION_FLAG : 0;
        flags |= value.isAsync ? IS_ASYNC_FLAG : 0;
        flags |= value.derivedConstructor ? DERIVED_CONSTRUCTOR_FLAG : 0;
        return flags;
    }

    @SuppressWarnings("unchecked")
    public static JSDescriptor<?> descriptorConstant(
            MethodHandles.Lookup lookup,
            String name,
            Class<?> type,
            JSCode code,
            JSCode constructor,
            JSDescriptor<?> parent,
            int flags,
            String sourceFile,
            SourceCodeProvider sourceCodeProvider,
            int sourceStart,
            int sourceEnd,
            String functionName,
            int languageVersion,
            int paramAndVarCount,
            int paramCount,
            int arity,
            int functionType,
            Object... args) {
        int i = 0;

        var paramAndVarNames = new String[(Integer) args[i++]];
        for (int p = 0; p < paramAndVarNames.length; p++) {
            paramAndVarNames[p] = ((String) args[i++]).intern();
        }
        var paramIsConst = new boolean[(Integer) args[i++]];
        for (int p = 0; p < paramIsConst.length; p++) {
            paramIsConst[p] = (Integer) args[i++] != 0;
        }
        var functionStatementNames = new String[args.length - i];
        for (int s = 0; s < functionStatementNames.length; s++) {
            functionStatementNames[s] = (String) args[i++];
        }

        return new JSDescriptor<>(
                code,
                constructor,
                parent,
                paramAndVarNames,
                paramIsConst,
                (flags & IS_STRICT_FLAG) != 0,
                (flags & IS_SCRIPT_FLAG) != 0,
                (flags & IS_TOP_LEVEL_FLAG) != 0,
                (flags & IS_ES6_GENERATOR_FLAG) != 0,
                (flags & IS_SHORTHAND_FLAG) != 0,
                (flags & HAS_PROTOTYPE_FLAG) != 0,
                (flags & HAS_LEXICAL_THIS_FLAG) != 0,
                (flags & IS_EVAL_FUNCTION_FLAG) != 0,
                (flags & HAS_REST_ARG_FLAG) != 0,
                sourceFile,
                sourceCodeProvider,
                sourceStart,
                sourceEnd,
                functionName == null ? null : functionName.intern(),
                languageVersion,
                paramAndVarCount,
                paramCount,
                arity,
                (flags & HAS_DEFAULT_PARAMETERS_FLAG) != 0,
                (flags & REQUIRES_ACTIVATION_FRAME_FLAG) != 0,
                (flags & REQUIRES_ARGUMENT_OBJECT_FLAG) != 0,
                (flags & DECLARED_AS_FUNCTION_EXPRESSION_FLAG) != 0,
                (flags & DERIVED_CONSTRUCTOR_FLAG) != 0,
                (flags & IS_ASYNC_FLAG) != 0,
                null,
                null,
                functionType,
                functionStatementNames);
    }
}
