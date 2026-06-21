package org.mozilla.javascript.interpreterv2.instruction;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ResultAccumulator;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;
import org.mozilla.javascript.interpreterv2.operand.Operand;
import org.mozilla.javascript.interpreterv2.operand.PopOperand;

/**
 * Pops a spread source off the stack and appends each of its own enumerable key/value pairs to the
 * {@link ResultAccumulator} on top of the stack, used for object spread properties.
 */
public class AccumulateKeyValues extends Instruction {
    public static final AccumulateKeyValues instance = new AccumulateKeyValues(PopOperand.instance);
    private final Operand operand;

    private AccumulateKeyValues(Operand operand) {
        this.operand = operand;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;
        Object source = operand.retrieveAndWrap(cx, frame);
        var results = (ResultAccumulator) frame.peek(0);
        ScriptRuntime.accumulateObjectKeyValues(cx, frame.scope, results, source);
    }

    @Override
    public int stackChange() {
        return operand.stackChange();
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(this, "source", operand);
    }
}
