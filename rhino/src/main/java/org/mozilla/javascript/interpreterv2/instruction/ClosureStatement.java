package org.mozilla.javascript.interpreterv2.instruction;

import static org.mozilla.javascript.InterpreterV2.initFunction;

import java.util.Objects;
import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;

public class ClosureStatement extends Instruction {
    private final int fnIndex;

    public ClosureStatement(int fnIndex) {
        this.fnIndex = fnIndex;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;
        initFunction(cx, frame.scope, frame.fnOrScript.getDescriptor(), fnIndex);
    }

    @Override
    public int stackChange() {
        return 0;
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(this, "fnIndex", fnIndex);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ClosureStatement)) {
            return false;
        }
        ClosureStatement other = (ClosureStatement) o;
        return fnIndex == other.fnIndex;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fnIndex);
    }
}
