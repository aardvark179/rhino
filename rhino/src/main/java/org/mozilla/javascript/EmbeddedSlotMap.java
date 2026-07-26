/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

/*
 * This class implements the SlotMap interface using an embedded hash table. This hash table
 * has the minimum overhead needed to get the job done. In particular, it embeds the Slot
 * directly into the hash table rather than creating an intermediate object, which seems
 * to have a measurable performance benefit.
 */

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

public class EmbeddedSlotMap<T extends PropHolder<T>> implements SlotMap<T> {

    private static class SlotHolder<T extends PropHolder<T>> {
        private final Slot<T> slot;
        private SlotHolder<T> next;
        private SlotHolder<T> orderedNext;

        private SlotHolder(Slot<T> slot) {
            this.slot = slot;
        }
    }

    private SlotHolder<T>[] slots;

    // gateways into the definition-order linked list of slots
    private SlotHolder<T> firstAdded;
    private SlotHolder<T> lastAdded;

    private int count;
    private boolean hasIndex = false;

    // initial slot array size, must be a power of 2
    private static final int INITIAL_SLOT_SIZE = 4;

    private static final class Iter<T extends PropHolder<T>> implements Iterator<Slot<T>> {
        private SlotHolder<T> next;

        Iter(SlotHolder<T> holder) {
            next = holder;
        }

        @Override
        public boolean hasNext() {
            return next != null;
        }

        @Override
        public Slot<T> next() {
            var ret = next;
            if (ret == null) {
                throw new NoSuchElementException();
            }
            next = next.orderedNext;
            return ret.slot;
        }
    }

    public EmbeddedSlotMap() {}

    @SuppressWarnings("unchecked")
    public EmbeddedSlotMap(int capacity) {
        int n = -1 >>> Integer.numberOfLeadingZeros(capacity - 1);
        n = (n < 0) ? 1 : n + 1;
        slots = new SlotHolder[n];
    }

    @Override
    public int size() {
        return count;
    }

    @Override
    public boolean isEmpty() {
        return count == 0;
    }

    @Override
    public Iterator<Slot<T>> iterator() {
        return new Iter<T>(firstAdded);
    }

    /** Locate the slot with the given name or index. */
    @Override
    public Slot<T> query(Object key, int index) {
        if (slots == null || (key == null && !hasIndex)) {
            return null;
        }

        int indexOrHash = (key != null ? key.hashCode() : index);
        int slotIndex = getSlotIndex(slots.length, indexOrHash);
        for (SlotHolder<T> holder = slots[slotIndex]; holder != null; holder = holder.next) {
            if (holder.slot.keyMatches(key, indexOrHash)) {
                return holder.slot;
            }
        }
        return null;
    }

    /**
     * Locate the slot with given name or index, and create a new one if necessary.
     *
     * @param key either a String or a Symbol object that identifies the property
     * @param index index or 0 if slot holds property name.
     */
    @Override
    public Slot<T> modify(SlotMapOwner<T> owner, Object key, int index, int attributes) {
        final int indexOrHash = (key != null ? key.hashCode() : index);
        if (slots != null) {
            final int slotIndex = getSlotIndex(slots.length, indexOrHash);
            for (var holder = slots[slotIndex]; holder != null; holder = holder.next) {
                if (holder.slot.keyMatches(key, indexOrHash)) {
                    return holder.slot;
                }
            }
        }
        var desc = new SimpleDescriptor<T, SlotMapOwner<T>>(key, index);
        var newSlot = desc.createSlot(null, null, attributes);
        createNewSlot(owner, newSlot);
        return newSlot;
    }

    @SuppressWarnings("unchecked")
    private void createNewSlot(SlotMapOwner<T> owner, Slot<T> newSlot) {
        if (count == 0 && slots == null) {
            // Always throw away old slots if any on empty insert.
            slots = new SlotHolder[INITIAL_SLOT_SIZE];
        }

        // Check if the table is not too full before inserting.
        if (4 * (count + 1) > 3 * slots.length) {
            // table size must be a power of 2 -- always grow by x2!
            if (count >= SlotMapOwner.LARGE_HASH_SIZE) {
                promoteMap(owner, newSlot);
                return;
            }
            SlotHolder<T>[] newSlots = new SlotHolder[slots.length * 2];
            copyTable(slots, newSlots);
            slots = newSlots;
        }

        insertNewSlot(newSlot);
    }

    protected void promoteMap(SlotMapOwner<T> owner, Slot<T> newSlot) {
        var newMap = new HashSlotMap<T>(this, newSlot);
        owner.setMap(newMap);
    }

    @Override
    public <S extends Slot<T>> S compute(
            SlotMapOwner<T> owner,
            CompoundOperationMap<T> compoundOp,
            Object key,
            int index,
            SlotComputer<S, T> c) {
        final int indexOrHash = (key != null ? key.hashCode() : index);

        if (slots != null) {
            SlotHolder<T> holder;
            final int slotIndex = getSlotIndex(slots.length, indexOrHash);
            SlotHolder<T> prev = slots[slotIndex];
            for (holder = prev; holder != null; holder = holder.next) {
                if (holder.slot.keyMatches(key, indexOrHash)) {
                    break;
                }
                prev = holder;
            }
            if (holder != null) {
                return computeExisting(owner, compoundOp, key, index, c, holder, prev, slotIndex);
            }
        }
        return computeNew(owner, compoundOp, key, index, c);
    }

    private <S extends Slot<T>> S computeNew(
            SlotMapOwner<T> owner,
            CompoundOperationMap<T> compoundOp,
            Object key,
            int index,
            SlotComputer<S, T> c) {
        S newSlot = c.compute(key, index, null, compoundOp, owner);
        if (newSlot != null) {
            if (!compoundOp.touched) {
                createNewSlot(owner, newSlot);
            } else {
                owner.getMap().add(owner, newSlot);
            }
        }
        return newSlot;
    }

    private <S extends Slot<T>> S computeExisting(
            SlotMapOwner<T> owner,
            CompoundOperationMap<T> compoundOp,
            Object key,
            int index,
            SlotComputer<S, T> c,
            SlotHolder<T> holder,
            SlotHolder<T> prev,
            int slotIndex) {
        // Modify or remove existing slot
        S newSlot = c.compute(key, index, holder.slot, compoundOp, owner);
        if (!compoundOp.touched) {
            if (newSlot == null) {
                // Need to delete this slot actually
                removeSlot(holder, prev, slotIndex, key);
            } else if (!Objects.equals(holder.slot, newSlot)) {
                // Replace slot in hash table
                var newHolder = new SlotHolder<>(newSlot);
                if (prev == holder) {
                    slots[slotIndex] = newHolder;
                } else {
                    prev.next = newHolder;
                }
                newHolder.next = holder.next;
                // Replace new slot in linked list, keeping same order
                if (holder == firstAdded) {
                    firstAdded = newHolder;
                } else {
                    SlotHolder<T> ph = firstAdded;
                    while ((ph != null) && (ph.orderedNext != holder)) {
                        ph = ph.orderedNext;
                    }
                    if (ph != null) {
                        ph.orderedNext = newHolder;
                    }
                }
                newHolder.orderedNext = holder.orderedNext;
                if (holder == lastAdded) {
                    lastAdded = newHolder;
                }
            }
            return newSlot;
        } else {
            return compoundOp.compute(
                    owner, compoundOp, key, slotIndex, (k, i, s, m, o) -> newSlot);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void add(SlotMapOwner<T> owner, Slot<T> newSlot) {
        if (slots == null) {
            slots = new SlotHolder[INITIAL_SLOT_SIZE];
        }
        createNewSlot(owner, newSlot);
    }

    private void insertNewSlot(Slot<T> newSlot) {
        ++count;
        // add new slot to linked list
        var newHolder = new SlotHolder<>(newSlot);
        if (lastAdded != null) {
            lastAdded.orderedNext = newHolder;
        }
        if (firstAdded == null) {
            firstAdded = newHolder;
        }
        lastAdded = newHolder;
        if (newSlot.getName() == null) hasIndex = true;
        addKnownAbsentSlot(slots, newHolder);
    }

    private void removeSlot(SlotHolder<T> holder, SlotHolder<T> prev, int ix, Object key) {
        count--;
        // remove slot from hash table
        if (prev == holder) {
            slots[ix] = holder.next;
        } else {
            prev.next = holder.next;
        }

        // remove from ordered list. Previously this was done lazily in
        // getIds() but delete is an infrequent operation so O(n)
        // should be ok

        // ordered list always uses the actual slot
        if (holder == firstAdded) {
            prev = null;
            firstAdded = holder.orderedNext;
        } else {
            prev = firstAdded;
            while (prev.orderedNext != holder) {
                prev = prev.orderedNext;
            }
            prev.orderedNext = holder.orderedNext;
        }
        if (holder == lastAdded) {
            lastAdded = prev;
        }
    }

    private static <T extends PropHolder<T>> void copyTable(
            SlotHolder<T>[] oldHolders, SlotHolder<T>[] newHolders) {
        for (var holder : oldHolders) {
            while (holder != null) {
                var nextHolder = holder.next;
                addKnownAbsentSlot(newHolders, holder);
                holder = nextHolder;
            }
        }
    }

    /**
     * Add slot with keys that are known to absent from the table. This is an optimization to use
     * when inserting into empty table, after table growth or during deserialization.
     */
    private static <T extends PropHolder<T>> void addKnownAbsentSlot(
            SlotHolder<T>[] addSlots, SlotHolder<T> holder) {
        final int insertPos = getSlotIndex(addSlots.length, holder.slot.getIndexOrHash());
        holder.next = addSlots[insertPos];
        addSlots[insertPos] = holder;
    }

    private static int getSlotIndex(int tableSize, int indexOrHash) {
        // This is a Java trick to efficiently "mod" the hash code by the table size.
        // It only works if the table size is a power of 2.
        // The performance improvement is measurable.
        return indexOrHash & (tableSize - 1);
    }
}
