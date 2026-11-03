package org.mozilla.javascript.interpreterv2.instruction;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.ClassLiteralDescriptor;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;

/**
 * Initializes the instance fields of the currently executing class constructor on {@code this}, and
 * pushes {@code this}.
 */
public class InitFields extends Instruction {
    public static final InitFields instance = new InitFields();

    private InitFields() {}

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;
        frame.push(
                ClassLiteralDescriptor.initializeFields(
                        cx, frame.scope, frame.fnOrScript, frame.thisObj));
    }

    @Override
    public int stackChange() {
        return 1;
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(this);
    }
}
