package org.mozilla.javascript;

public class NativeLexicalScope extends NativeScope {
    private static final long serialVersionUID = -7471457301304454454L;

    public NativeLexicalScope(JSScope parentScope) {
        super(parentScope);
    }

    @Override
    public boolean isBoundaryScope() {
        return false;
    }
}
