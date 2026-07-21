package org.mozilla.javascript.interpreterv2.instruction;

import java.util.Objects;
import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;

public class NameIncDec extends Instruction {
    private final String name;
    private final int incrDecrMask;

    public NameIncDec(String name, int incrDecrMask) {
        this.name = name;
        this.incrDecrMask = incrDecrMask;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.push(ScriptRuntime.nameIncrDecr(frame.scope, name, cx, incrDecrMask));

        frame.pc += 1;
    }

    @Override
    public int stackChange() {
        return 1;
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(this, "name", name, "mask", incrDecrMask);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof NameIncDec)) {
            return false;
        }
        NameIncDec other = (NameIncDec) o;
        return Objects.equals(name, other.name) && incrDecrMask == other.incrDecrMask;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, incrDecrMask);
    }
}
