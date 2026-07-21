package org.mozilla.javascript.interpreterv2.instruction;

import java.util.Objects;
import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.interpreterv2.operand.Operand;

public class RefNsName extends Instruction {
    private final Operand namespace;
    private final Operand name;
    private final int flags;

    public RefNsName(Operand namespace, Operand name, int flags) {
        this.namespace = namespace;
        this.name = name;
        this.flags = flags;
    }

    @Override
    public void interpret(Context cx, CallFrameV2 frame) {
        frame.pc += 1;

        Object nameValue = name.retrieveAndWrap(cx, frame);
        Object namespaceValue = namespace.retrieveAndWrap(cx, frame);

        Object result = ScriptRuntime.nameRef(namespaceValue, nameValue, cx, frame.scope, flags);
        frame.push(result);
    }

    @Override
    public int stackChange() {
        return 1 + namespace.stackChange() + name.stackChange();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RefNsName)) {
            return false;
        }
        RefNsName other = (RefNsName) o;
        return Objects.equals(namespace, other.namespace)
                && Objects.equals(name, other.name)
                && flags == other.flags;
    }

    @Override
    public int hashCode() {
        return Objects.hash(namespace, name, flags);
    }
}
