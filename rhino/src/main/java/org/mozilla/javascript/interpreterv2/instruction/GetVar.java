package org.mozilla.javascript.interpreterv2.instruction;

import java.util.Objects;
import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;

public class GetVar extends Instruction {
    private final int index;

    private static final GetVar[] CACHE = new GetVar[16];

    static {
        for (int i = 0; i < CACHE.length; i++) {
            CACHE[i] = new GetVar(i);
        }
    }

    public static GetVar createInstruction(int index) {
        if (index >= 0 && index < CACHE.length) {
            return CACHE[index];
        }
        return new GetVar(index);
    }

    private GetVar(int index) {
        this.index = index;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;

        frame.push(frame.getVar(index), frame.getVarDouble(index));
    }

    @Override
    public int stackChange() {
        return 1;
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(this, "index", index);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GetVar)) {
            return false;
        }
        GetVar other = (GetVar) o;
        return index == other.index;
    }

    @Override
    public int hashCode() {
        return Objects.hash(index);
    }
}
