package org.mozilla.javascript.interpreterv2.instruction;

import java.util.ArrayList;
import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.interpreterv2.operand.Operand;

/** Arity-specialized plain {@link Call} with two arguments. */
final class Call2Spread extends Call {
    private final Operand arg0;
    private final Operand arg1;

    Call2Spread(Operand lookupResult, Operand arg0, Operand arg1) {
        super(lookupResult);
        this.arg0 = arg0;
        this.arg1 = arg1;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;
        countInvocation(cx, frame);
        // Retrieve in reverse order: arguments sit on the stack with the last on top.
        var args = new ArrayList<Object>();
        args.add(arg1.retrieveAndWrap(cx, frame));
        spreadArray(cx, frame.scope, args, arg0.retrieveAndWrap(cx, frame));
        doPlainCall(cx, frame, args.toArray());
    }

    @Override
    public int stackChange() {
        return 1 + lookupResult.stackChange() + arg0.stackChange() + arg1.stackChange();
    }

    @Override
    public String toDebugString() {
        return formatDebug(new Operand[] {arg0, arg1});
    }
}
