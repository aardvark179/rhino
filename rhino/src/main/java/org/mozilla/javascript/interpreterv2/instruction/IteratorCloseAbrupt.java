package org.mozilla.javascript.interpreterv2.instruction;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;
import org.mozilla.javascript.interpreterv2.operand.Operand;

public class IteratorCloseAbrupt extends Instruction {
    private final Operand op;

    public IteratorCloseAbrupt(Operand op) {
        this.op = op;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        ScriptRuntime.closeIteratorAbrupt(op.retrieve(cx, frame), cx, frame.scope);
        frame.push(Undefined.instance);

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
