package org.mozilla.javascript.interpreterv2.instruction;

import java.util.Objects;
import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.interpreterv2.operand.Operand;

/** Arity-specialized plain {@link Call} with no arguments. */
final class Call0 extends Call {
    Call0(Operand lookupResult) {
        super(lookupResult);
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;
        countInvocation(cx, frame);
        doPlainCall(cx, frame, ScriptRuntime.emptyArgs);
    }

    @Override
    public int stackChange() {
        return 1 + lookupResult.stackChange();
    }

    @Override
    public String toDebugString() {
        return formatDebug(Operand.EMPTY_ARRAY);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Call0)) {
            return false;
        }
        Call0 other = (Call0) o;
        return Objects.equals(lookupResult, other.lookupResult);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lookupResult);
    }
}
