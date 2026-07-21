package org.mozilla.javascript.interpreterv2.instruction;

import java.util.Objects;
import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.interpreterv2.operand.Operand;

/** Arity-specialized plain {@link Call} with three arguments. */
final class Call3 extends Call {
    private final Operand arg0;
    private final Operand arg1;
    private final Operand arg2;

    Call3(Operand lookupResult, Operand arg0, Operand arg1, Operand arg2) {
        super(lookupResult);
        this.arg0 = arg0;
        this.arg1 = arg1;
        this.arg2 = arg2;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;
        countInvocation(cx, frame);
        // Retrieve in reverse order: arguments sit on the stack with the last on top.
        Object[] args = new Object[3];
        args[2] = arg2.retrieveAndWrap(cx, frame);
        args[1] = arg1.retrieveAndWrap(cx, frame);
        args[0] = arg0.retrieveAndWrap(cx, frame);
        doPlainCall(cx, frame, args);
    }

    @Override
    public int stackChange() {
        return 1
                + lookupResult.stackChange()
                + arg0.stackChange()
                + arg1.stackChange()
                + arg2.stackChange();
    }

    @Override
    public String toDebugString() {
        return formatDebug(new Operand[] {arg0, arg1, arg2});
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Call3)) {
            return false;
        }
        Call3 other = (Call3) o;
        return Objects.equals(lookupResult, other.lookupResult)
                && Objects.equals(arg0, other.arg0)
                && Objects.equals(arg1, other.arg1)
                && Objects.equals(arg2, other.arg2);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lookupResult, arg0, arg1, arg2);
    }
}
