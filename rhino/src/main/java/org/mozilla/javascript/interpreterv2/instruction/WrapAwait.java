package org.mozilla.javascript.interpreterv2.instruction;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;
import org.mozilla.javascript.interpreterv2.operand.Operand;

public class WrapAwait extends Instruction {
    private final Operand op;

    public WrapAwait(Operand op) {
        this.op = op;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.push(ScriptRuntime.wrapAwait(op.retrieve(cx, frame)));

        frame.pc += 1;
    }

    @Override
    public int stackChange() {
        return 1 + op.stackChange();
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(this, "op", op);
    }
}
