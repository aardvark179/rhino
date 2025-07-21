package org.mozilla.javascript;

import static java.util.Map.entry;

import java.io.Serializable;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

public class MapShape implements Serializable {

    private static final long serialVersionUID = 0L;

    public static MapShape EMPTY_SHAPE = new MapShape(new SlotDescriptor[0]);

    // Not going to specify what sort of map yet.
    private final Map<Object, SlotDescriptor> descriptorMap;
    // Putting these in an array because we can then access them by
    // index easily. (Needed for error reporting). Also, it turns out
    // that because arguments can be given the _same_ name, and
    // although they are accessible via `arguments`, and should
    // probably be visible in a debugger, the name should on ly
    // resolve to the last one.
    private final SlotDescriptor[] descriptors;

    private MapShape(SlotDescriptor[] descriptors) {
        Map.Entry<Object, SlotDescriptor>[] entries = new Map.Entry[descriptors.length];
        for (int i = 0; i < descriptors.length; i++) {
            entries[i] = entry(descriptors[i].id, descriptors[i]);
        }
        descriptorMap = Map.ofEntries(entries);
        this.descriptors = descriptors;
    }

    private MapShape(SlotDescriptor[] descriptors, SlotDescriptor newSlot) {
        Map.Entry<Object, SlotDescriptor>[] entries = new Map.Entry[descriptors.length];
        for (int i = 0; i < descriptors.length; i++) {
            // If we're adding a slot with a duplicate name (e.g. a
            // repeated argument name) then we only want the new one
            // in the map.
            if (descriptors[i] == newSlot || descriptors[i].id != newSlot.id) {
                entries[i] = entry(descriptors[i].id, descriptors[i]);
            }
        }
        descriptorMap = Map.ofEntries(entries);
        this.descriptors = descriptors;
    }

    private MapShape(LinkedHashMap<Object, SlotDescriptor> orderedMap) {
        descriptors = new SlotDescriptor[orderedMap.size()];
        int i = 0;
        for (var e : orderedMap.entrySet()) {
            descriptors[i++] = e.getValue();
        }
        descriptorMap = Map.copyOf(orderedMap);
    }

    SlotDescriptor get(Object id) {
        return descriptorMap.get(id);
    }

    SlotDescriptor getByIndex(int index) {
        return descriptors[index];
    }

    public int size() {
        return descriptors.length;
    }

    public MapShape createMapAdding(Object id, int attributes) {
        int offset = descriptors.length;
        SlotDescriptor[] newDescriptors =
                Arrays.copyOf(descriptors, descriptors.length + 1, SlotDescriptor[].class);
        var newDescriptor = new SlotDescriptor(id, offset, attributes);
        newDescriptors[newDescriptors.length - 1] = newDescriptor;
        return new MapShape(newDescriptors, newDescriptor);
    }

    public static class Builder {
        private final LinkedHashMap<Object, SlotDescriptor> descriptors = new LinkedHashMap<>();
        private int offset = 0;

        public Builder withSlot(Object id, int attributes) {
            descriptors.put(id, new SlotDescriptor(id, offset, attributes));
            return this;
        }

        public MapShape build() {
            return new MapShape(descriptors);
        }
    }
}

class SlotDescriptor implements Serializable {
    final int offset;
    final int attributes;
    final Object id;
    final int hashCode; // Not sure we really need or want this.

    SlotDescriptor(Object id, int offset, int attributes) {
        this.id = id;
        this.offset = offset;
        this.attributes = attributes;
        int h = id.hashCode();
        h = 31 * h + offset;
        h = 31 * h + attributes;
        this.hashCode = h;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SlotDescriptor) {
            SlotDescriptor other = (SlotDescriptor) obj;
            return other.id.equals(id) && other.offset == offset && other.attributes == attributes;
        } else {
            return false;
        }
    }
}
