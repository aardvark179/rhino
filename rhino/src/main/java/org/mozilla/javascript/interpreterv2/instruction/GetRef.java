package org.mozilla.javascript.interpreterv2.instruction;

import java.util.Objects;
import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Ref;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;
import org.mozilla.javascript.interpreterv2.operand.Operand;

public class GetRef extends Instruction {
    private final Operand obj;

    public GetRef(Operand obj) {
        this.obj = obj;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;

        var ref = (Ref) this.obj.retrieve(cx, frame);
        frame.push(ScriptRuntime.refGet(ref, cx));
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
        if (!(o instanceof GetRef)) {
            return false;
        }
        GetRef other = (GetRef) o;
        return Objects.equals(obj, other.obj);
    }

    @Override
    public int hashCode() {
        return Objects.hash(obj);
    }
}
