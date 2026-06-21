package org.mozilla.javascript.interpreterv2.instruction;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ResultAccumulator;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;

/**
 * Pushes a {@link ResultAccumulator} onto the stack, pre-sized for the expected number of results,
 * which the following accumulate instructions fill before a {@link MakeArray} or {@link MakeObject}
 * consumes it.
 */
public class ResultAccumulatorInstruction extends Instruction {
    private final int capacity;

    public ResultAccumulatorInstruction(int capacity) {
        this.capacity = capacity;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.push(new ResultAccumulator(capacity));
        frame.pc += 1;
    }

    @Override
    public int stackChange() {
        return 1;
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(this, "capacity", capacity);
    }
}
