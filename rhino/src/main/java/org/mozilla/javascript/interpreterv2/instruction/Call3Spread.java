package org.mozilla.javascript.interpreterv2.instruction;

import java.util.ArrayList;
import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.interpreterv2.operand.Operand;

/** Arity-specialized plain {@link Call} with three arguments. */
final class Call3Spread extends Call {
    private final Operand arg0;
    private final Operand arg1;
    private final Operand arg2;

    Call3Spread(Operand lookupResult, Operand arg0, Operand arg1, Operand arg2) {
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
        var args = new ArrayList<Object>();
        var args2 = arg2.retrieveAndWrap(cx, frame);
        var args1 = arg1.retrieveAndWrap(cx, frame);
        var args0 = arg0.retrieveAndWrap(cx, frame);
        args.add(args0);
        args.add(args1);
        spreadArray(cx, frame.scope, args, args2);
        doPlainCall(cx, frame, args.toArray());
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
}
