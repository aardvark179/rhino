package org.mozilla.javascript.interpreterv2.instruction;

import java.util.Objects;
import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;
import org.mozilla.javascript.interpreterv2.operand.Operand;

public class EnumInitValues extends Instruction {
    private final Operand obj;
    private final int index;

    public EnumInitValues(Operand obj, int index) {
        this.obj = obj;
        this.index = index;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;

        var lhs = obj.retrieveAndWrap(cx, frame);
        frame.setLocal(
                index,
                ScriptRuntime.enumInit(lhs, cx, frame.scope, ScriptRuntime.ENUMERATE_VALUES));
    }

    @Override
    public int stackChange() {
        return obj.stackChange();
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(this, "obj", obj, "index", index);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EnumInitValues)) {
            return false;
        }
        EnumInitValues other = (EnumInitValues) o;
        return Objects.equals(obj, other.obj) && index == other.index;
    }

    @Override
    public int hashCode() {
        return Objects.hash(obj, index);
    }
}
