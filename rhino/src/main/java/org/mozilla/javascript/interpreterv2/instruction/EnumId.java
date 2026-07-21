package org.mozilla.javascript.interpreterv2.instruction;

import java.util.Objects;
import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;

public class EnumId extends Instruction {
    private final int localBlockRef;

    public EnumId(int localBlockRef) {
        this.localBlockRef = localBlockRef;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        Object val = frame.getLocal(localBlockRef);
        frame.push(ScriptRuntime.enumId(val, cx));

        frame.pc += 1;
    }

    @Override
    public int stackChange() {
        return 1;
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(this, "localBlockRef", localBlockRef);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EnumId)) {
            return false;
        }
        EnumId other = (EnumId) o;
        return localBlockRef == other.localBlockRef;
    }

    @Override
    public int hashCode() {
        return Objects.hash(localBlockRef);
    }
}
