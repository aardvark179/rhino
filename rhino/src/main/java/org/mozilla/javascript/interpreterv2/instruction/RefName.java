package org.mozilla.javascript.interpreterv2.instruction;

import java.util.Objects;
import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.interpreterv2.operand.Operand;

public class RefName extends Instruction {
    private final Operand name;
    private final int flags;

    public RefName(Operand name, int flags) {
        this.name = name;
        this.flags = flags;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;

        Object nameValue = name.retrieveAndWrap(cx, frame);

        Object result = ScriptRuntime.nameRef(nameValue, cx, frame.scope, flags);
        frame.push(result);
    }

    @Override
    public int stackChange() {
        return 1 + name.stackChange();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RefName)) {
            return false;
        }
        RefName other = (RefName) o;
        return Objects.equals(name, other.name) && flags == other.flags;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, flags);
    }
}
