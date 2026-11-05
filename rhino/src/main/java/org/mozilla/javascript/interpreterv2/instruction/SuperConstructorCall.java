package org.mozilla.javascript.interpreterv2.instruction;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;
import org.mozilla.javascript.interpreterv2.operand.Operand;

/**
 * A {@code super(...)} constructor call. Per GetSuperConstructor (ES2015 13.3.7.3), the callee is
 * the currently executing (class constructor) function's own {@code [[Prototype]]}; it is
 * constructed with the current {@code new.target} and the result is bound as {@code this}.
 */
public final class SuperConstructorCall extends Instruction {
    private final Operand[] arguments;

    public SuperConstructorCall(Operand[] arguments) {
        this.arguments = arguments;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;
        Object[] args = frame.getArguments(cx, arguments);
        Scriptable result =
                ScriptRuntime.superConstruct(
                        cx, frame.scope, frame.fnOrScript, frame.thisObj, frame.newTarget, args);
        frame.thisObj = result;
        frame.push(result);
    }

    @Override
    public int stackChange() {
        int count = 0;
        for (var argument : arguments) {
            count += argument.stackChange();
        }
        return 1 + count;
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(this, "args", arguments);
    }
}
