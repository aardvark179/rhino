package org.mozilla.javascript.interpreterv2.instruction;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.ClassLiteralDescriptor;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;
import org.mozilla.javascript.interpreterv2.operand.Operand;

/**
 * Initializes the instance fields of the currently executing class constructor on {@code this}, as
 * described by a {@link ClassLiteralDescriptor}, and pushes {@code this}.
 */
public class InitFields extends Instruction {
    private final Operand descriptorOp;

    public InitFields(Operand descriptorOp) {
        this.descriptorOp = descriptorOp;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;
        var descriptor = (ClassLiteralDescriptor) descriptorOp.retrieve(cx, frame);
        frame.push(descriptor.initializeFields(cx, frame.scope, frame.fnOrScript, frame.thisObj));
    }

    @Override
    public int stackChange() {
        return 1 + descriptorOp.stackChange();
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(this, "descriptor", descriptorOp);
    }
}
