package org.mozilla.javascript.interpreterv2.instruction;

import java.util.Objects;
import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.interpreterv2.operand.Operand;

/** Arity-specialized plain {@link Call} with a single argument. */
final class Call1 extends Call {
    private final Operand arg0;

    Call1(Operand lookupResult, Operand arg0) {
        super(lookupResult);
        this.arg0 = arg0;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;
        countInvocation(cx, frame);
        Object[] args = {arg0.retrieveAndWrap(cx, frame)};
        doPlainCall(cx, frame, args);
    }

    @Override
    public int stackChange() {
        return 1 + lookupResult.stackChange() + arg0.stackChange();
    }

    @Override
    public String toDebugString() {
        return formatDebug(new Operand[] {arg0});
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Call1)) {
            return false;
        }
        Call1 other = (Call1) o;
        return Objects.equals(lookupResult, other.lookupResult) && Objects.equals(arg0, other.arg0);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lookupResult, arg0);
    }
}
