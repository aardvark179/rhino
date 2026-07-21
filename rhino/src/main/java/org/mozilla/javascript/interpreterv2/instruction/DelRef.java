package org.mozilla.javascript.interpreterv2.instruction;

import java.util.Objects;
import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Ref;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;
import org.mozilla.javascript.interpreterv2.operand.Operand;

public class DelRef extends Instruction {
    private final Operand obj;

    public DelRef(Operand obj) {
        this.obj = obj;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;

        var ref = (Ref) obj.retrieve(cx, frame);
        frame.push(ScriptRuntime.refDel(ref, cx));
    }

    @Override
    public int stackChange() {
        return 1 + obj.stackChange();
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(this, "obj", obj.getClass().getSimpleName());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DelRef)) {
            return false;
        }
        DelRef other = (DelRef) o;
        return Objects.equals(obj, other.obj);
    }

    @Override
    public int hashCode() {
        return Objects.hash(obj);
    }
}
