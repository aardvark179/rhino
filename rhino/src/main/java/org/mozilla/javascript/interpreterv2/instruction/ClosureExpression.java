package org.mozilla.javascript.interpreterv2.instruction;

import java.util.Objects;
import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.JSFunction;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;
import org.mozilla.javascript.interpreterv2.operand.Operand;

public class ClosureExpression extends Instruction {
    private final int fnIndex;
    private final Operand lexThisOp;
    private final Operand homeObjOp;
    private final Operand newTargetOp;

    public static ClosureExpression createInstruction(
            int fnIndex, Operand lexThisOp, Operand homeObjOp, Operand newTargetOp) {
        return new ClosureExpression(fnIndex, lexThisOp, homeObjOp, newTargetOp);
    }

    private ClosureExpression(
            int fnIndex, Operand lexThisOp, Operand homeObjOp, Operand newTargetOp) {
        this.fnIndex = fnIndex;
        this.lexThisOp = lexThisOp;
        this.homeObjOp = homeObjOp;
        this.newTargetOp = newTargetOp;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;

        var desc = frame.fnOrScript.getDescriptor();
        var fdesc = desc.getFunction(fnIndex);
        var lexicalThis = lexThisOp.retrieve(cx, frame);
        Scriptable homeObject = (Scriptable) homeObjOp.retrieve(cx, frame);
        var newTarget = newTargetOp.retrieve(cx, frame);

        JSFunction fn = new JSFunction(cx, frame.scope, fdesc, lexicalThis, newTarget, homeObject);
        frame.push(fn);
    }

    @Override
    public int stackChange() {
        return 1;
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(
                this,
                "fnIndex",
                fnIndex,
                "lexicalThis",
                lexThisOp,
                "homeObject",
                homeObjOp,
                "newTarget",
                newTargetOp);
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
