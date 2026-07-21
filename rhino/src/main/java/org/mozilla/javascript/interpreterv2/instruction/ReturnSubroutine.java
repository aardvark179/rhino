package org.mozilla.javascript.interpreterv2.instruction;

import static org.mozilla.javascript.InterpreterV2.addInstructionCount;

import java.util.Objects;
import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;

public class ReturnSubroutine extends Instruction {
    private final int returnPcOffset;

    public ReturnSubroutine(int returnPcOffset) {
        this.returnPcOffset = returnPcOffset;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;

        if (cx.getInstructionObserverThreshold() != 0) {
            addInstructionCount(cx, frame, 0);
        }

        // Normal return from GOSUB
        if (frame.hasSubRoutineReturnPC(returnPcOffset)) {
            var returnPc = frame.getSubRoutineReturnPC(returnPcOffset);
            frame.pc = (int) returnPc;
            if (cx.getInstructionObserverThreshold() != 0) {
                frame.pcPrevBranch = frame.pc;
            }
            return;
        }

        // Invocation from exception handler, restore object to rethrow
        frame.throwable = frame.getLocal(returnPcOffset);
    }

    @Override
    public int stackChange() {
        return 0;
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(this, "returnPcOffset", returnPcOffset);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ReturnSubroutine)) {
            return false;
        }
        ReturnSubroutine other = (ReturnSubroutine) o;
        return returnPcOffset == other.returnPcOffset;
    }

    @Override
    public int hashCode() {
        return Objects.hash(returnPcOffset);
    }
}
