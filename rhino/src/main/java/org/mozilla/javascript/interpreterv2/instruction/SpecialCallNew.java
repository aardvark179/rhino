package org.mozilla.javascript.interpreterv2.instruction;

import static org.mozilla.javascript.InterpreterV2.INVOCATION_COST;
import static org.mozilla.javascript.InterpreterV2.addInstructionCount;

import java.util.Arrays;
import java.util.Objects;
import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;
import org.mozilla.javascript.interpreterv2.operand.Operand;

public class SpecialCallNew extends Instruction {
    private final Operand fun;
    private final Operand[] arguments;
    private final int callType;

    public SpecialCallNew(Operand fun, Operand[] arguments, int callType) {
        this.fun = fun;
        this.arguments = arguments;
        this.callType = callType;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;

        if (cx.getInstructionObserverThreshold() != 0) {
            addInstructionCount(cx, frame, INVOCATION_COST);
        }

        Object[] args = frame.getArguments(cx, arguments);
        Object function = fun.retrieveAndWrap(cx, frame);
        frame.push(ScriptRuntime.newSpecial(cx, function, args, frame.scope, callType));
    }

    @Override
    public int stackChange() {
        int count = 0;
        for (var argument : arguments) {
            count += argument.stackChange();
        }
        return 1 + fun.stackChange() + count;
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(
                this, "fun", fun, "args", arguments, "callType", callType);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SpecialCallNew)) {
            return false;
        }
        SpecialCallNew other = (SpecialCallNew) o;
        return Objects.equals(fun, other.fun)
                && Arrays.equals(arguments, other.arguments)
                && callType == other.callType;
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(fun, callType);
        result = 31 * result + Arrays.hashCode(arguments);
        return result;
    }
}
