package org.mozilla.javascript.interpreterv2.instruction;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;

/** Pushes a fresh empty object onto the stack, used as the base for an object literal. */
public class EmptyObject extends Instruction {
    public static final EmptyObject instance = new EmptyObject();

    private EmptyObject() {}

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.push(cx.newObject(frame.scope));
        frame.pc += 1;
    }

    @Override
    public int stackChange() {
        return 1;
    }
}
