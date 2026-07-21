package org.mozilla.javascript.interpreterv2.instruction;

import java.util.Objects;
import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;

public class Rethrow extends Instruction {
    private final int localBlockRef;

    public Rethrow(int localBlockRef) {
        this.localBlockRef = localBlockRef;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;

        frame.throwable = frame.getLocal(localBlockRef);
    }

    @Override
    public int stackChange() {
        return 0;
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
        if (!(o instanceof Rethrow)) {
            return false;
        }
        Rethrow other = (Rethrow) o;
        return localBlockRef == other.localBlockRef;
    }

    @Override
    public int hashCode() {
        return Objects.hash(localBlockRef);
    }
}
