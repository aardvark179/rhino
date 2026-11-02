package org.mozilla.javascript.interpreterv2.instruction;

import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.ClassLiteralDescriptor;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ResultAccumulator;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;
import org.mozilla.javascript.interpreterv2.operand.Operand;

/**
 * Consumes the {@link ResultAccumulator}, the prototype object, and the superclass value beneath it
 * on the stack, then pushes the class (constructor) value assembled by the {@link
 * ClassLiteralDescriptor} from the accumulated constructor/method results.
 */
public class MakeClass extends Instruction {
    private final Operand descriptorOp;
    private final Operand resultsOp;
    private final Operand prototypeOp;
    private final Operand superClassOp;

    public MakeClass(
            Operand descriptorOp, Operand resultsOp, Operand prototypeOp, Operand superClassOp) {
        this.descriptorOp = descriptorOp;
        this.resultsOp = resultsOp;
        this.prototypeOp = prototypeOp;
        this.superClassOp = superClassOp;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;
        var descriptor = (ClassLiteralDescriptor) descriptorOp.retrieve(cx, frame);
        var results = (ResultAccumulator) resultsOp.retrieve(cx, frame);
        var prototype = (Scriptable) prototypeOp.retrieve(cx, frame);
        var superClass = superClassOp.retrieve(cx, frame);
        frame.push(
                descriptor.createClass(
                        cx, frame.scope, superClass, prototype, results.getResults()));
    }

    @Override
    public int stackChange() {
        return 1
                + descriptorOp.stackChange()
                + resultsOp.stackChange()
                + prototypeOp.stackChange()
                + superClassOp.stackChange();
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(
                this,
                "descriptor",
                descriptorOp,
                "results",
                resultsOp,
                "prototype",
                prototypeOp,
                "superClass",
                superClassOp);
    }
}
