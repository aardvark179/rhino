package org.mozilla.classfile;

import java.lang.constant.ConstantDesc;
import java.util.HashMap;
import java.util.Map;

public class DynamicDescriberRegistry {

    private final Map<Class<?>, DynamicConstantDescriber<?>> registry;

    DynamicDescriberRegistry() {
        registry = new HashMap<>();
    }

    void register(DynamicConstantDescriber<?> describer) {
        registry.put(describer.describedType(), describer);
    }

    private final Map<Object, ConstantDesc> cached = new HashMap<>();

    public <T> ConstantDesc describe(T obj) {
        if (obj instanceof ConstantDesc cd) {
            return cd;
        }
        ConstantDesc res = cached.get(obj);
        if (res != null) {
            return res;
        }

        @SuppressWarnings("unchecked")
        var type = (Class<T>) obj.getClass();
        @SuppressWarnings("unchecked")
        var describer = (DynamicConstantDescriber<T>) registry.get(type);
        if (describer == null) {
            throw new IllegalArgumentException(String.format("No describer found for %s.", type));
        }
        res = describer.describe(obj);
        cached.put(obj, res);
        return res;
    }
}
