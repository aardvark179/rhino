package org.mozilla.javascript.interpreterv2.instruction;

import org.mozilla.javascript.ArrayLiteralDescriptor;
import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ResultAccumulator;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;
import org.mozilla.javascript.interpreterv2.operand.Operand;

/**
 * Consumes the {@link ResultAccumulator} on top of the stack and replaces it with the array
 * produced by the {@link ArrayLiteralDescriptor}.
 */
public class MakeArray extends Instruction {
    private final Operand descriptorOp;
    private final Operand resultsOp;

    public MakeArray(Operand descriptorOp, Operand resultsOp) {
        this.descriptorOp = descriptorOp;
        this.resultsOp = resultsOp;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;
        var descriptor = (ArrayLiteralDescriptor) descriptorOp.retrieve(cx, frame);
        var results = (ResultAccumulator) resultsOp.retrieve(cx, frame);
        frame.push(descriptor.createArray(cx, frame.scope, results));
    }

    @Override
    public int stackChange() {
        return 0;
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(
                this, "descriptor", descriptorOp, "results", resultsOp);
    }
}
