package org.mozilla.javascript.interpreterv2.instruction;

import static org.mozilla.javascript.InterpreterV2.addInstructionCount;

import java.util.Objects;
import java.util.Set;
import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;

public class GoSubroutine extends JumpInstruction {
    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.push((double) frame.pc + 1);
        if (cx.getInstructionObserverThreshold() != 0) {
            addInstructionCount(cx, frame, 2);
        }
        frame.pc += getOffset();
        frame.pcPrevBranch = frame.pc;
    }

    @Override
    public int stackChange() {
        // The return address is pushed for the subroutine, which pops it again
        // before it returns to the next instruction.
        return 0;
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(
                this, "target", InstructionFormatter.formatTarget(getOffset()));
    }

    @Override
    public Set<Integer> getTargets(int fromPC) {
        // The next instruction is where the subroutine returns to.
        return Set.of(fromPC + getOffset(), fromPC + 1);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GoSubroutine)) {
            return false;
        }
        GoSubroutine other = (GoSubroutine) o;
        return getOffset() == other.getOffset();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getOffset());
    }
}
