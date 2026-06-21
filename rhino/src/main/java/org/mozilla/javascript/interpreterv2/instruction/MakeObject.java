package org.mozilla.javascript.interpreterv2.instruction;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ObjectLiteralDescriptor;
import org.mozilla.javascript.ResultAccumulator;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;
import org.mozilla.javascript.interpreterv2.operand.Operand;

/**
 * Consumes the {@link ResultAccumulator} and the object beneath it on the stack, then pushes the
 * object populated by the {@link ObjectLiteralDescriptor} from the accumulated key/value results.
 */
public class MakeObject extends Instruction {
    private final Operand descriptorOp;
    private final Operand resultsOp;
    private final Operand objOp;

    public MakeObject(Operand descriptorOp, Operand resultsOP, Operand objOp) {
        this.descriptorOp = descriptorOp;
        this.resultsOp = resultsOP;
        this.objOp = objOp;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;
        var descriptor = (ObjectLiteralDescriptor) descriptorOp.retrieve(cx, frame);
        var results = (ResultAccumulator) resultsOp.retrieve(cx, frame);
        var obj = (Scriptable) objOp.retrieve(cx, frame);
        frame.push(descriptor.createObject(cx, frame.scope, obj, results.getResults()));
    }

    @Override
    public int stackChange() {
        return 1 + descriptorOp.stackChange() + resultsOp.stackChange() + objOp.stackChange();
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(
                this, "descriptor", descriptorOp, "results", resultsOp, "obj", objOp);
    }
}
