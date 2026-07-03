package org.mozilla.javascript.interpreterv2.instruction;

import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.ClassLiteralDescriptor;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;
import org.mozilla.javascript.interpreterv2.operand.Operand;

/**
 * Consumes the constructor and superclass value beneath it on the stack, then pushes the class
 * value assembled by the {@link ClassLiteralDescriptor}.
 */
public class MakeClass extends Instruction {
    private final Operand descriptorOp;
    private final Operand constructorOp;
    private final Operand superClassOp;

    public MakeClass(Operand descriptorOp, Operand constructorOp, Operand superClassOp) {
        this.descriptorOp = descriptorOp;
        this.constructorOp = constructorOp;
        this.superClassOp = superClassOp;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;
        var descriptor = (ClassLiteralDescriptor) descriptorOp.retrieve(cx, frame);
        var constructor = (BaseFunction) constructorOp.retrieve(cx, frame);
        var superClass = superClassOp.retrieve(cx, frame);
        frame.push(
                descriptor.createClass(
                        cx, frame.scope, superClass, constructor, ScriptRuntime.emptyArgs));
    }

    @Override
    public int stackChange() {
        return 1
                + descriptorOp.stackChange()
                + constructorOp.stackChange()
                + superClassOp.stackChange();
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(
                this,
                "descriptor",
                descriptorOp,
                "constructor",
                constructorOp,
                "superClass",
                superClassOp);
    }
}
