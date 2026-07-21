package org.mozilla.javascript.interpreterv2.instruction;

import java.util.Objects;
import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;
import org.mozilla.javascript.interpreterv2.operand.Operand;

public class Cleanup extends Instruction {
    private final Operand object;

    public Cleanup(Operand object) {
        this.object = object;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;
        object.cleanup(frame);
    }

    @Override
    public int stackChange() {
        return 0;
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(this, "object", object);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Cleanup)) {
            return false;
        }
        Cleanup other = (Cleanup) o;
        return Objects.equals(object, other.object);
    }

    @Override
    public int hashCode() {
        return Objects.hash(object);
    }
}
