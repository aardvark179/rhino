package org.mozilla.javascript.interpreterv2.instruction;

import static org.mozilla.javascript.InterpreterV2.INVOCATION_COST;
import static org.mozilla.javascript.InterpreterV2.addInstructionCount;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ResultAccumulator;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;
import org.mozilla.javascript.interpreterv2.operand.Operand;

/**
 * Special call ({@code eval} etc.) whose arguments were gathered into a {@link ResultAccumulator}.
 */
public class SpecialCallAccumulated extends Instruction {
    private final Operand lookupResult;
    private final Operand accumulator;
    private final short lineNumber;
    private final int callType;

    public SpecialCallAccumulated(
            Operand lookupResult, Operand accumulator, short lineNumber, int callType) {
        this.lookupResult = lookupResult;
        this.accumulator = accumulator;
        this.lineNumber = lineNumber;
        this.callType = callType;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;

        if (cx.getInstructionObserverThreshold() != 0) {
            addInstructionCount(cx, frame, INVOCATION_COST);
        }

        Object[] args = ((ResultAccumulator) accumulator.retrieve(cx, frame)).getCallArgs();
        var result = (ScriptRuntime.LookupResult) lookupResult.retrieve(cx, frame);
        Callable function = result.getCallable();

        frame.push(
                ScriptRuntime.callSpecial(
                        cx,
                        function,
                        result.getThis(),
                        args,
                        frame.scope,
                        frame.thisObj,
                        callType,
                        frame.fnOrScript.getDescriptor().getSourceName(),
                        lineNumber,
                        false));
    }

    @Override
    public int stackChange() {
        return 1 + lookupResult.stackChange() + accumulator.stackChange();
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(
                this,
                "lookupResult",
                lookupResult,
                "accumulator",
                accumulator,
                "line",
                lineNumber,
                "callType",
                callType);
    }
}
