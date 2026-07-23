package org.mozilla.javascript;

import java.util.ArrayList;
import java.util.List;

public class SlotMapDescriptor<T extends PropHolder<T>, O extends SlotMapOwner<T>> {

    private final List<CompactSlot.Descriptor<?, T, O>> slots;
    private final int[] attributes;

    private SlotMapDescriptor(List<CompactSlot.Descriptor<?, T, O>> slots, int[] attributes) {
        this.slots = slots;
        this.attributes = attributes;
    }

    SlotMap<T> buildMap(O owner) {
        return SlotMapOwner.createSlotMap(
                slots.size() > 0 ? slots.get(0).createSlot(owner, attributes[0]) : null,
                slots.size() > 1 ? slots.get(1).createSlot(owner, attributes[1]) : null,
                slots.size() > 2 ? slots.get(2).createSlot(owner, attributes[2]) : null,
                slots.size() > 3 ? slots.get(3).createSlot(owner, attributes[3]) : null);
    }

    public void installMap(O owner) {
        owner.setMap(buildMap(owner));
    }

    public static class Builder<T extends PropHolder<T>, O extends SlotMapOwner<T>> {
        List<CompactSlot.Descriptor<?, T, O>> slots = new ArrayList<>();
        List<Integer> attributes = new ArrayList<>();

        public Builder() {}

        public Builder(CompactSlot.Descriptor<?, T, O> descriptor, int attributes) {
            slots.add(descriptor);
            this.attributes.add(attributes);
        }

        private Builder(SlotMapDescriptor<T, O> old) {
            slots = new ArrayList<>(old.slots);
            attributes = new ArrayList<>(old.attributes.length * 2);
            for (int i = 0; i < old.attributes.length; i++) {
                attributes.add(old.attributes[i]);
            }
        }

        public static <T extends PropHolder<T>, O extends SlotMapOwner<T>>
                Builder<T, O> startingWith(
                        CompactSlot.Descriptor<?, T, O> descriptor, int attributes) {
            return new Builder<>(descriptor, attributes);
        }

        public static <T extends PropHolder<T>, O extends SlotMapOwner<T>> Builder<T, O> extending(
                SlotMapDescriptor<T, O> start) {
            return new Builder<>(start);
        }

        public Builder<T, O> withSlot(CompactSlot.Descriptor<?, T, O> descriptor, int attributes) {
            slots.add(descriptor);
            this.attributes.add(attributes);
            return this;
        }

        public SlotMapDescriptor<T, O> build() {
            return new SlotMapDescriptor<>(
                    List.copyOf(slots), attributes.stream().mapToInt(v -> v).toArray());
        }
    }
}
