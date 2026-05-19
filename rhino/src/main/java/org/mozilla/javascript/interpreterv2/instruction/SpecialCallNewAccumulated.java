package org.mozilla.javascript.interpreterv2.instruction;

import static org.mozilla.javascript.InterpreterV2.INVOCATION_COST;
import static org.mozilla.javascript.InterpreterV2.addInstructionCount;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ResultAccumulator;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;
import org.mozilla.javascript.interpreterv2.operand.Operand;

/** Special {@code new} whose arguments were gathered into a {@link ResultAccumulator}. */
public class SpecialCallNewAccumulated extends Instruction {
    private final Operand fun;
    private final Operand accumulator;
    private final int callType;

    public SpecialCallNewAccumulated(Operand fun, Operand accumulator, int callType) {
        this.fun = fun;
        this.accumulator = accumulator;
        this.callType = callType;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;

        if (cx.getInstructionObserverThreshold() != 0) {
            addInstructionCount(cx, frame, INVOCATION_COST);
        }

        Object[] args = ((ResultAccumulator) accumulator.retrieve(cx, frame)).getCallArgs();
        Object function = fun.retrieveAndWrap(cx, frame);
        frame.push(ScriptRuntime.newSpecial(cx, function, args, frame.scope, callType));
    }

    @Override
    public int stackChange() {
        return 1 + fun.stackChange() + accumulator.stackChange();
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(
                this, "fun", fun, "accumulator", accumulator, "callType", callType);
    }
}
