package org.mozilla.javascript;

import java.lang.invoke.MethodHandles;

public class NullSourceCodeProvider implements SourceCodeProvider {
    static final NullSourceCodeProvider NULL_PROVIDER = new NullSourceCodeProvider();

    private NullSourceCodeProvider() {}

    @Override
    public String getSource(JSDescriptor<?> desc, int start, int end) {
        return String.format(
                "function %s() {\n\t/* Source unavailable */\n}", desc.getFunctionName());
    }

    @Override
    public String getRawSource() {
        return String.format("/* Source unavailable */\n");
    }

    /**
     * Bootstrap method for the dynamic constant described by {@link
     * org.mozilla.javascript.optimizer.NullSourceCodeProviderDescriber}.
     */
    public static NullSourceCodeProvider sourceConstant(
            MethodHandles.Lookup lookup, String name, Class<?> type) {
        return NULL_PROVIDER;
    }
}
