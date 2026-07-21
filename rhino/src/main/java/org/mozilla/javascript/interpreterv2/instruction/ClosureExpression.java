package org.mozilla.javascript.interpreterv2.instruction;

import java.util.Objects;
import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.JSFunction;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.ast.FunctionNode;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;

public class ClosureExpression extends Instruction {
    private final int fnIndex;

    private static final ClosureExpression[] CACHE = new ClosureExpression[16];

    static {
        for (int i = 0; i < CACHE.length; i++) {
            CACHE[i] = new ClosureExpression(i);
        }
    }

    public static ClosureExpression createInstruction(int fnIndex) {
        if (fnIndex >= 0 && fnIndex < CACHE.length) {
            return CACHE[fnIndex];
        }
        return new ClosureExpression(fnIndex);
    }

    private ClosureExpression(int fnIndex) {
        this.fnIndex = fnIndex;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;

        var desc = frame.fnOrScript.getDescriptor();
        var fdesc = desc.getFunction(fnIndex);
        boolean isArrow = fdesc.getFunctionType() == FunctionNode.ARROW_FUNCTION;
        Object lexicalThis = isArrow ? frame.thisObj : null;
        var homeObject = isArrow ? frame.fnOrScript.getHomeObject() : null;
        var newTarget = isArrow ? frame.newTarget : Undefined.instance;

        JSFunction fn = new JSFunction(cx, frame.scope, fdesc, lexicalThis, newTarget, homeObject);
        frame.push(fn);
    }

    @Override
    public int stackChange() {
        return 1;
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(this, "fnIndex", fnIndex);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ClosureExpression)) {
            return false;
        }
        ClosureExpression other = (ClosureExpression) o;
        return fnIndex == other.fnIndex;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fnIndex);
    }
}
