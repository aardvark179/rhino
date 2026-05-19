package org.mozilla.javascript.interpreterv2.instruction;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ResultAccumulator;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;
import org.mozilla.javascript.interpreterv2.operand.Operand;

/**
 * {@code new} whose arguments were gathered into a {@link ResultAccumulator} on top of the stack,
 * used when a {@code new} expression has a spread argument in any position.
 */
public final class NewAccumulated extends New {
    private final Operand accumulator;

    public NewAccumulated(Operand fun, Operand accumulator) {
        super(fun);
        this.accumulator = accumulator;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;
        countInvocation(cx, frame);
        Object[] args = ((ResultAccumulator) accumulator.retrieve(cx, frame)).getCallArgs();
        doNew(cx, frame, args);
    }

    @Override
    public int stackChange() {
        return 1 + fun.stackChange() + accumulator.stackChange();
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(
                "NewAccumulated", "fun", fun, "accumulator", accumulator);
    }
}
