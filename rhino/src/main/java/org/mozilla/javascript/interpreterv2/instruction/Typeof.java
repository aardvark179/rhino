package org.mozilla.javascript.interpreterv2.instruction;

import java.util.Objects;
import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;
import org.mozilla.javascript.interpreterv2.operand.Operand;

public class Typeof extends Instruction {
    private final Operand obj;

    public Typeof(Operand obj) {
        this.obj = obj;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;

        var obj = this.obj.retrieveAndWrap(cx, frame);
        frame.push(ScriptRuntime.typeof(obj));
    }

    @Override
    public int stackChange() {
        return 1 + obj.stackChange();
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(this, "obj", obj);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Typeof)) {
            return false;
        }
        Typeof other = (Typeof) o;
        return Objects.equals(obj, other.obj);
    }

    @Override
    public int hashCode() {
        return Objects.hash(obj);
    }
}
