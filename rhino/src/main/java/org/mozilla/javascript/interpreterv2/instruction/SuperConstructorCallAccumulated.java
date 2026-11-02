package org.mozilla.javascript.interpreterv2.instruction;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ResultAccumulator;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;
import org.mozilla.javascript.interpreterv2.operand.Operand;

/**
 * A {@code super(...)} constructor call whose arguments were gathered into a {@link
 * ResultAccumulator} on top of the stack (used when the call has a spread argument in any
 * position). See {@link SuperConstructorCall} for the non-spread case and callee/this semantics.
 */
public final class SuperConstructorCallAccumulated extends Instruction {
    private final Operand accumulator;

    public SuperConstructorCallAccumulated(Operand accumulator) {
        this.accumulator = accumulator;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;
        Object[] args = ((ResultAccumulator) accumulator.retrieve(cx, frame)).getCallArgs();
        Callable superConstructor = (Callable) ((Scriptable) frame.fnOrScript).getPrototype();
        frame.push(superConstructor.call(cx, frame.scope, frame.thisObj, args));
    }

    @Override
    public int stackChange() {
        return 1 + accumulator.stackChange();
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(this, "accumulator", accumulator);
    }
}
