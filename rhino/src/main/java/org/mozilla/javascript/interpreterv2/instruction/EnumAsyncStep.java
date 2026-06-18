package org.mozilla.javascript.interpreterv2.instruction;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;
import org.mozilla.javascript.interpreterv2.operand.Operand;

public class EnumAsyncStep extends Instruction {
    private final int localBlockRef;
    private final Operand op;

    public EnumAsyncStep(Operand op, int localBlockRef) {
        this.op = op;
        this.localBlockRef = localBlockRef;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        Object val = frame.getLocal(localBlockRef);
        frame.push(ScriptRuntime.enumAsyncStep(val, op.retrieve(cx, frame), cx));

        frame.pc += 1;
    }

    @Override
    public int stackChange() {
        return 1 + op.stackChange();
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(this, "localBlockRef", localBlockRef);
    }
}
