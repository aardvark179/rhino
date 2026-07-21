package org.mozilla.javascript.interpreterv2.instruction;

import java.util.Objects;
import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;
import org.mozilla.javascript.interpreterv2.operand.Operand;

public class SetElem extends Instruction {
    private final Operand lhs;
    private final Operand elem;
    private final Operand rhs;

    public SetElem(Operand lhs, Operand elem, Operand rhs) {
        this.lhs = lhs;
        this.elem = elem;
        this.rhs = rhs;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;

        var rhs = this.rhs.retrieveAndWrap(cx, frame);
        Object value;
        if (!this.elem.isDouble(frame)) {
            var id = this.elem.retrieve(cx, frame);
            var lhs = this.lhs.retrieveAndWrap(cx, frame);
            value = ScriptRuntime.setObjectElem(lhs, id, rhs, cx, frame.scope);
        } else {
            double d = this.elem.retrieveDouble(frame);
            var lhs = this.lhs.retrieveAndWrap(cx, frame);
            value = ScriptRuntime.setObjectIndex(lhs, d, rhs, cx, frame.scope);
        }

        frame.push(value);
    }

    @Override
    public int stackChange() {
        return 1 + lhs.stackChange() + elem.stackChange() + rhs.stackChange();
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(this, "lhs", lhs, "elem", elem, "rhs", rhs);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SetElem)) {
            return false;
        }
        SetElem other = (SetElem) o;
        return Objects.equals(lhs, other.lhs)
                && Objects.equals(elem, other.elem)
                && Objects.equals(rhs, other.rhs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lhs, elem, rhs);
    }
}
