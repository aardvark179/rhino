package org.mozilla.javascript.interpreterv2.operand;

import java.util.Objects;
import org.mozilla.javascript.CallFrameV2;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.interpreterv2.InstructionSimplification;
import org.mozilla.javascript.interpreterv2.KnownType;

public final class DoubleOperand extends Operand {

    private final double value;

    public DoubleOperand(double value) {
        this.value = value;
    }

    @Override
    public Double retrieve(Context cx, CallFrameV2 frame) {
        return value;
    }

    @Override
    public double retrieveDouble(CallFrameV2 frame) {
        return value;
    }

    @Override
    public boolean isDouble(CallFrameV2 frame) {
        return true;
    }

    @Override
    public KnownType getKnownType(InstructionSimplification simplifier) {
        return KnownType.NUMBER;
    }

    @Override
    public void appendDebugString(StringBuilder sb) {
        sb.append(value);
    }

    @Override
    public boolean isValidJumpTableKey() {
        return true;
    }

    @Override
    public boolean coerceToBoolean(Context cx, CallFrameV2 frame) {
        return !Double.isNaN(value) && value != 0.0;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DoubleOperand)) {
            return false;
        }
        DoubleOperand other = (DoubleOperand) o;
        return Double.compare(value, other.value) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
