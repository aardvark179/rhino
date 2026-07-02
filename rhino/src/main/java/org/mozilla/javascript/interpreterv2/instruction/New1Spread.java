package org.mozilla.javascript.interpreterv2.instruction;

import java.util.ArrayList;
import java.util.Objects;
import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.interpreterv2.operand.Operand;

/** Arity-specialized {@link New} with a single argument. */
final class New1Spread extends New {
    private final Operand arg0;

    New1Spread(Operand fun, Operand arg0) {
        super(fun);
        this.arg0 = arg0;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;
        countInvocation(cx, frame);
        var args = new ArrayList<Object>();
        spreadArray(cx, frame.scope, args, arg0.retrieveAndWrap(cx, frame));
        doNew(cx, frame, args.toArray());
    }

    @Override
    public int stackChange() {
        return 1 + fun.stackChange() + arg0.stackChange();
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
        if (!(o instanceof New1Spread)) {
            return false;
        }
        New1Spread other = (New1Spread) o;
        return Objects.equals(fun, other.fun) && Objects.equals(arg0, other.arg0);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fun, arg0);
    }
}
