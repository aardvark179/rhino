package org.mozilla.javascript.interpreterv2.instruction;

import java.util.Objects;
import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;
import org.mozilla.javascript.interpreterv2.operand.Operand;

public class ElemIncDec extends Instruction {
    private final Operand obj;
    private final Operand elem;
    private final int incrDecrMask;

    public ElemIncDec(Operand obj, Operand elem, int incrDecrMask) {
        this.obj = obj;
        this.elem = elem;
        this.incrDecrMask = incrDecrMask;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;

        var elem = this.elem.retrieveAndWrap(cx, frame);
        var obj = this.obj.retrieveAndWrap(cx, frame);

        frame.push(ScriptRuntime.elemIncrDecr(obj, elem, cx, frame.scope, incrDecrMask));
    }

    @Override
    public int stackChange() {
        return 1 + obj.stackChange() + elem.stackChange();
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(
                this,
                "obj",
                obj.getClass().getSimpleName(),
                "elem",
                elem.getClass().getSimpleName(),
                "mask",
                incrDecrMask);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ElemIncDec)) {
            return false;
        }
        ElemIncDec other = (ElemIncDec) o;
        return Objects.equals(obj, other.obj)
                && Objects.equals(elem, other.elem)
                && incrDecrMask == other.incrDecrMask;
    }

    @Override
    public int hashCode() {
        return Objects.hash(obj, elem, incrDecrMask);
    }
}
