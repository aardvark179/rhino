package org.mozilla.javascript.interpreterv2.instruction;

import java.util.Objects;
import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;
import org.mozilla.javascript.interpreterv2.operand.Operand;

public class PropAndThis extends Instruction {
    private final Operand obj;
    private final String property;

    public PropAndThis(Operand obj, String property) {
        this.obj = obj;
        this.property = property;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;

        var obj = this.obj.retrieveAndWrap(cx, frame);
        frame.push(ScriptRuntime.getPropAndThis(obj, property, cx, frame.scope));
    }

    @Override
    public int stackChange() {
        return 1 + obj.stackChange();
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(this, "name", property);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PropAndThis)) {
            return false;
        }
        PropAndThis other = (PropAndThis) o;
        return Objects.equals(obj, other.obj) && Objects.equals(property, other.property);
    }

    @Override
    public int hashCode() {
        return Objects.hash(obj, property);
    }
}
