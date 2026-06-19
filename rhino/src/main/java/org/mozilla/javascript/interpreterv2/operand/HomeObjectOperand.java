package org.mozilla.javascript.interpreterv2.operand;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;

public final class HomeObjectOperand extends Operand {
    public static final HomeObjectOperand instance = new HomeObjectOperand();

    private HomeObjectOperand() {}

    @Override
    public Object retrieve(Context cx, CallFrameV2 frame) {
        return frame.fnOrScript.getHomeObject();
    }

    @Override
    public double retrieveDouble(CallFrameV2 frame) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isDouble(CallFrameV2 frame) {
        return false;
    }

    @Override
    public void appendDebugString(StringBuilder sb) {
        sb.append("this");
    }
}
