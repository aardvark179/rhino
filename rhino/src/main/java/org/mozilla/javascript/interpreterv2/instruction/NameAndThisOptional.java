package org.mozilla.javascript.interpreterv2.instruction;

import java.util.Objects;
import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.interpreterv2.InstructionFormatter;

public class NameAndThisOptional extends Instruction {
    private final String name;

    public NameAndThisOptional(String name) {
        this.name = name;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;

        frame.push(ScriptRuntime.getNameAndThisOptional(name, cx, frame.scope));
    }

    @Override
    public int stackChange() {
        return 1;
    }

    @Override
    public String toDebugString() {
        return InstructionFormatter.formatInstruction(this, "name", name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof NameAndThisOptional)) {
            return false;
        }
        NameAndThisOptional other = (NameAndThisOptional) o;
        return Objects.equals(name, other.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
