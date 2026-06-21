package org.mozilla.javascript.interpreterv2.instruction;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ResultAccumulator;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;
import org.mozilla.javascript.interpreterv2.operand.Operand;
import org.mozilla.javascript.interpreterv2.operand.PopOperand;

/**
 * Pops a single value off the stack and appends it to the {@link ResultAccumulator} that now sits
 * on top of the stack.
 */
public class AccumulateResult extends Instruction {
    public static final AccumulateResult instance = new AccumulateResult(PopOperand.instance);

    private final Operand operand;

    private AccumulateResult(Operand operand) {
        this.operand = operand;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;
        Object value = operand.retrieveAndWrap(cx, frame);
        ((ResultAccumulator) frame.peek(0)).addResult(value);
    }

    @Override
    public int stackChange() {
        return operand.stackChange();
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(this, "value", operand);
    }
}
