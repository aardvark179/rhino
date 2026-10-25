package org.mozilla.javascript;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.lang.ref.WeakReference;
import java.util.Objects;

/**
 * Can provide source code from a lazy {@link SourceCodeSupplier}, that for example can retrieve it
 * from the database.
 */
public class LazySourceCodeProvider implements SourceCodeProvider, Serializable {
    private static final long serialVersionUID = 1L;
    private SourceCodeSupplier sourceSupplier;
    private WeakReference<String> sourceCodeRef;

    public LazySourceCodeProvider(SourceCodeSupplier sourceSupplier) {
        Objects.requireNonNull(sourceSupplier);
        this.sourceSupplier = sourceSupplier;
    }

    private LazySourceCodeProvider() {}

    public SourceCodeSupplier getSourceSupplier() {
        return sourceSupplier;
    }

    /**
     * Set the supplier of a provider created by {@link #sourceConstant}. The supplier cannot be
     * written into a class file, so a provider resolved from a dynamic constant starts without one,
     * and must have it set once the class has been defined and before any source is requested.
     */
    public void setSourceSupplier(SourceCodeSupplier sourceSupplier) {
        Objects.requireNonNull(sourceSupplier);
        if (this.sourceSupplier != null) {
            throw new IllegalStateException("Source supplier has already been set.");
        }
        this.sourceSupplier = sourceSupplier;
    }

    /**
     * Bootstrap method for the dynamic constant described by {@link
     * org.mozilla.javascript.optimizer.LazySourceCodeProviderDescriber}. The provider has no
     * supplier until one is set with {@link #setSourceSupplier}.
     */
    public static LazySourceCodeProvider sourceConstant(
            MethodHandles.Lookup lookup, String name, Class<?> type) {
        return new LazySourceCodeProvider();
    }

    @Override
    public String getSource(JSDescriptor<?> descriptor, int start, int end) {
        String source;
        if (sourceCodeRef != null && (source = sourceCodeRef.get()) != null) {
            var functionSrc = source.substring(start, end);
            descriptor.replaceSourceProvider(new ResolvedSourceProvider(functionSrc));
            return functionSrc;
        } else {
            source = sourceSupplier.get();
            if (source != null) {
                sourceCodeRef = new WeakReference<>(source);
                var functionSrc = source.substring(start, end);
                descriptor.replaceSourceProvider(new ResolvedSourceProvider(functionSrc));
                return functionSrc;
            } else {
                descriptor.replaceSourceProvider(new ResolvedSourceProvider(""));
                return "";
            }
        }
    }

    @Override
    public String getRawSource() {
        return sourceSupplier.get();
    }

    static class ResolvedSourceProvider implements SourceCodeProvider {

        private String source;

        private ResolvedSourceProvider(String source) {
            this.source = source;
        }

        @Override
        public String getSource(JSDescriptor<?> functionName, int start, int end) {
            return source;
        }

        @Override
        public String getRawSource() {
            return source;
        }
    }

    @Serial
    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        Context.checkDeserializationEnabled();
        stream.defaultReadObject();
    }

    @Serial
    private void writeObject(ObjectOutputStream stream) throws IOException {
        Context.checkSerializationEnabled();
        stream.defaultWriteObject();
    }

    @Serial
    private void readObjectNoData() throws InvalidObjectException {
        throw new InvalidObjectException("Missing serialized Rhino object data");
    }
}
