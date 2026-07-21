package org.mozilla.javascript.interpreterv2.instruction;

import java.util.Objects;
import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;
import org.mozilla.javascript.interpreterv2.operand.Operand;

public class ValueAndThis extends Instruction {
    private final Operand value;

    public ValueAndThis(Operand value) {
        this.value = value;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;

        var value = this.value.retrieveAndWrap(cx, frame);
        frame.push(ScriptRuntime.getValueAndThis(value, cx));
    }

    @Override
    public int stackChange() {
        return 1 + value.stackChange();
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(this, "value", value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ValueAndThis)) {
            return false;
        }
        ValueAndThis other = (ValueAndThis) o;
        return Objects.equals(value, other.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
