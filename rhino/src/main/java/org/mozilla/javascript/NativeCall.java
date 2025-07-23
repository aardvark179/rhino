/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

/**
 * This class implements the activation object.
 *
 * <p>See ECMA 10.1.6
 *
 * @see org.mozilla.javascript.Arguments
 * @author Norris Boyd
 */
public final class NativeCall extends ScriptableObject {
    private static final long serialVersionUID = -7471457301304454454L;

    private static final Object CALL_TAG = "Call";

    static void init(Scriptable scope, boolean sealed) {}

    NativeCall() {}

    NativeCall(
            NativeFunction function,
            Context cx,
            Scriptable scope,
            MapShape shape,
            Object[] args,
            boolean isArrow,
            boolean isStrict,
            boolean argsHasRest,
            boolean requiresArgumentObject,
            Scriptable homeObject) {
        this.function = function;
        this.homeObject = homeObject;
        this.isArrow = isArrow;

        setParentScope(scope);
        // leave prototype null

        this.originalArgs = (args == null) ? ScriptRuntime.emptyArgs : args;
        this.isStrict = isStrict;

        // initialize values of arguments
        int paramAndVarCount = function.getParamAndVarCount();
        int paramCount = function.getParamCount();
        if (paramAndVarCount != 0) {
            if (argsHasRest) {
                Object[] vals;
                if (args.length >= paramCount) {
                    vals = new Object[args.length - paramCount];
                    System.arraycopy(args, paramCount, vals, 0, args.length - paramCount);
                } else {
                    vals = ScriptRuntime.emptyArgs;
                }

                for (int i = 0; i < paramCount; ++i) {
                    String name = function.getParamOrVarName(i);
                    Object val = i < args.length ? args[i] : Undefined.instance;
                    defineProperty(name, val, PERMANENT);
                }
                defineProperty(
                        function.getParamOrVarName(paramCount),
                        cx.newArray(scope, vals),
                        PERMANENT);
            } else {
                for (int i = 0; i < paramCount; ++i) {
                    String name = function.getParamOrVarName(i);
                    Object val = i < args.length ? args[i] : Undefined.instance;
                    defineProperty(name, val, PERMANENT);
                }
            }
        }

        if (requiresArgumentObject) {
            // initialize "arguments" property but only if it was not overridden by
            // the parameter with the same name
            if (!super.has("arguments", this) && !isArrow) {
                arguments = new Arguments(this);
                defineProperty("arguments", arguments, PERMANENT);
            }
        }

        if (paramAndVarCount != 0) {
            for (int i = paramCount; i < paramAndVarCount; ++i) {
                String name = function.getParamOrVarName(i);
                if (!super.has(name, this)) {
                    if (function.getParamOrVarConst(i)) {
                        defineProperty(name, Undefined.instance, CONST);
                    } else if (!(function instanceof InterpretedFunction)
                            || ((InterpretedFunction) function).hasFunctionNamed(name)) {
                        defineProperty(name, Undefined.instance, PERMANENT);
                    }
                }
            }
        }
    }

    @Override
    public String getClassName() {
        return "Call";
    }

    public void defineAttributesForArguments() {
        if (arguments != null) {
            arguments.defineAttributesForStrictMode();
        }
    }

    public Scriptable getHomeObject() {
        return homeObject;
    }

    private static final int Id_constructor = 1, MAX_PROTOTYPE_ID = 1;

    NativeFunction function;
    Object[] originalArgs;
    boolean isStrict;
    private Arguments arguments;
    boolean isArrow;
    private Scriptable homeObject;

    transient NativeCall parentActivationCall;
}
