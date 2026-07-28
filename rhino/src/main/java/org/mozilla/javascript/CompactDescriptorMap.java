/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

/*
 * This class implements an ordered map of CompactSlot.Descriptor instances using an embedded
 * hash table, following the same design as EmbeddedSlotMap. The descriptor is embedded directly
 * into the hash table rather than creating an intermediate object, and the definition order of
 * the descriptors is preserved via a linked list. Only add and query are supported.
 */
public class CompactDescriptorMap<
        U extends PropHolder<U>,
        O extends SlotMapOwner<U>> {

    private static class DescriptorHolder<U extends PropHolder<U>, O extends SlotMapOwner<U>> {
        private final CompactSlot.Descriptor<?, U, O> descriptor;
        private DescriptorHolder<U, O> next;
        private DescriptorHolder<U, O> orderedNext;

        private DescriptorHolder(CompactSlot.Descriptor<?, U, O> descriptor) {
            this.descriptor = descriptor;
        }
    }

    private DescriptorHolder<U, O>[] descriptors;

    // gateways into the definition-order linked list of descriptors
    private DescriptorHolder<U, O> firstAdded;
    private DescriptorHolder<U, O> lastAdded;

    private int count;
    private boolean hasIndex = false;

    // initial table size, must be a power of 2
    private static final int INITIAL_SLOT_SIZE = 4;

    private static final class Iter<U extends PropHolder<U>, O extends SlotMapOwner<U>>
            implements Iterator<CompactSlot.Descriptor<?, U, O>> {
        private DescriptorHolder<U, O> next;

        Iter(DescriptorHolder<U, O> holder) {
            next = holder;
        }

        @Override
        public boolean hasNext() {
            return next != null;
        }

        @Override
        public CompactSlot.Descriptor<?, U, O> next() {
            var ret = next;
            if (ret == null) {
                throw new NoSuchElementException();
            }
            next = next.orderedNext;
            return ret.descriptor;
        }
    }

    private CompactDescriptorMap(
            DescriptorHolder<U, O>[] descriptors,
            DescriptorHolder<U,O> first,
            DescriptorHolder<U,O> last,
            int count,
            boolean hasIndex) {
        this.descriptors = descriptors;
        this.firstAdded = first;
        this.lastAdded = last;
        this.count = count;
        this.hasIndex = hasIndex;
    }

    public int size() {
        return count;
    }

    public boolean isEmpty() {
        return count == 0;
    }

    public Iterator<CompactSlot.Descriptor<?, U, O>> iterator() {
        return new Iter<U, O>(firstAdded);
    }

    /** Locate the descriptor with the given name or index. */
    public CompactSlot.Descriptor<?, U, O> query(Object key, int index) {
        if (descriptors == null || (key == null && !hasIndex)) {
            return null;
        }

        int indexOrHash = (key != null ? key.hashCode() : index);
        int slotIndex = getSlotIndex(descriptors.length, indexOrHash);
        for (DescriptorHolder<U, O> holder = descriptors[slotIndex];
                holder != null;
                holder = holder.next) {
            if (matches(holder.descriptor, key, indexOrHash)) {
                return holder.descriptor;
            }
        }
        return null;
    }

    private boolean matches(
            CompactSlot.Descriptor<?, U, O> descriptor, Object key, int indexOrHash) {
        return indexOrHash == descriptor.getIndexOrHash()
                && Objects.equals(descriptor.getName(), key);
    }

    private static int getSlotIndex(int tableSize, int indexOrHash) {
        // This is a Java trick to efficiently "mod" the hash code by the table size.
        // It only works if the table size is a power of 2.
        // The performance improvement is measurable.
        return indexOrHash & (tableSize - 1);
    }

    public static class Builder<U extends PropHolder<U>, O extends SlotMapOwner<U>> {
        private DescriptorHolder<U, O>[] descriptors;

        // gateways into the definition-order linked list of descriptors
        private DescriptorHolder<U, O> firstAdded;
        private DescriptorHolder<U, O> lastAdded;

        private int count;
        private boolean hasIndex = false;

        public Builder() {}

        private Builder(CompactSlot.Descriptor<?, U, O> descriptor) {
            withDescriptor(descriptor);
        }

        public static <U extends PropHolder<U>, O extends SlotMapOwner<U>>
                Builder<U, O> startingWith(CompactSlot.Descriptor<?, U, O> descriptor) {
            return new Builder<>(descriptor);
        }

        /** Add a descriptor to the map, preserving definition order. */
        @SuppressWarnings("unchecked")
        public void withDescriptor(CompactSlot.Descriptor<?, U, O> descriptor) {
            if (descriptors == null) {
                descriptors = new DescriptorHolder[INITIAL_SLOT_SIZE];
            }

            // Check if the table is not too full before inserting.
            if (4 * (count + 1) > 3 * descriptors.length) {
                // table size must be a power of 2 -- always grow by x2!
                DescriptorHolder<U, O>[] newDescriptors = new DescriptorHolder[descriptors.length * 2];
                copyTable(descriptors, newDescriptors);
                descriptors = newDescriptors;
            }

            insertNewDescriptor(descriptor);
        }

        public CompactDescriptorMap<U, O> build() {
            return new CompactDescriptorMap<>(descriptors, firstAdded, lastAdded, count, hasIndex);
        }

        private void insertNewDescriptor(CompactSlot.Descriptor<?, U, O> descriptor) {
            ++count;
            // add new descriptor to the ordered linked list
            var newHolder = new DescriptorHolder<U, O>(descriptor);
            if (lastAdded != null) {
                lastAdded.orderedNext = newHolder;
            }
            if (firstAdded == null) {
                firstAdded = newHolder;
            }
            lastAdded = newHolder;
            if (descriptor.getName() == null) hasIndex = true;
            addKnownAbsentDescriptor(descriptors, newHolder);
        }

        private void copyTable(
                DescriptorHolder<U, O>[] oldHolders, DescriptorHolder<U, O>[] newHolders) {
            for (var holder : oldHolders) {
                while (holder != null) {
                    var nextHolder = holder.next;
                    addKnownAbsentDescriptor(newHolders, holder);
                    holder = nextHolder;
                }
            }
        }

        /**
         * Add a descriptor with keys that are known to be absent from the table. This is an
         * optimization to use when inserting into an empty table or after table growth.
         */
        private void addKnownAbsentDescriptor(
                DescriptorHolder<U, O>[] addDescriptors, DescriptorHolder<U, O> holder) {
            final int insertPos =
                    getSlotIndex(addDescriptors.length, holder.descriptor.getIndexOrHash());
            holder.next = addDescriptors[insertPos];
            addDescriptors[insertPos] = holder;
        }
    }
}
