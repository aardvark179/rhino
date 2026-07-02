package org.mozilla.javascript.interpreterv2.instruction;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ResultAccumulator;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;
import org.mozilla.javascript.interpreterv2.operand.Operand;

/**
 * Call whose arguments were gathered into a {@link ResultAccumulator} on top of the stack, used
 * when a call has a spread argument in any position. The accumulator is popped first (it sits above
 * the lookup result), then the callable is invoked directly with the flattened arguments.
 */
public final class CallAccumulated extends Call {
    private final Operand accumulator;
    private final Call.Type callType;

    public CallAccumulated(Operand lookupResult, Operand accumulator, Call.Type callType) {
        super(lookupResult);
        this.accumulator = accumulator;
        this.callType = callType;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;
        countInvocation(cx, frame);

        Object[] args = ((ResultAccumulator) accumulator.retrieve(cx, frame)).getCallArgs();

        if (callType == Call.Type.Call || callType == Call.Type.TailCall) {
            doPlainCall(cx, frame, args);
            return;
        }

        var result = (ScriptRuntime.LookupResult) lookupResult.retrieve(cx, frame);
        Object funThisObj = result.getThis();
        Callable fun = result.getCallable();
        if (callType == Call.Type.CallOnSuper) {
            funThisObj = frame.thisObj;
        }

        if (callType == Call.Type.RefCall) {
            frame.push(ScriptRuntime.callRef(fun, funThisObj, args, cx));
            return;
        }

        cx.lastInterpreterFrame = frame;
        frame.push(fun.call(cx, frame.scope, funThisObj, args));
    }

    @Override
    public int stackChange() {
        return 1 + lookupResult.stackChange() + accumulator.stackChange();
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(
                "CallAccumulated",
                "callType",
                callType,
                "lookupResult",
                lookupResult,
                "accumulator",
                accumulator);
    }
}
