/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * This class holds the various SlotMaps of various types, and knows how to atomically switch
 * between them when we need to so that we use the right data structure at the right time.
 */
class SlotMapContainer implements SlotMap, SlotMapOwner {

    /**
     * Once the object has this many properties in it, we will replace the EmbeddedSlotMap with
     * HashSlotMap. We can adjust this parameter to balance performance for typical objects versus
     * performance for huge objects with many collisions.
     */
    static final int LARGE_HASH_SIZE = 2000;

    static final int DEFAULT_SIZE = 10;

    private static final class EmptySlotMap implements SlotMap {

        @Override
        public Iterator<Slot> iterator() {
            return Collections.emptyIterator();
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public Slot modify(SlotMapOwner container, Object key, int index, int attributes) {
            var map = new SingleSlotMap();
            container.replaceMap(map);
            return map.modify(container, key, index, attributes);
        }

        @Override
        public Slot query(Object key, int index) {
            return null;
        }

        @Override
        public void add(SlotMapOwner container, Slot newSlot) {
            var map = new SingleSlotMap();
            container.replaceMap(map);
            map.add(container, newSlot);
        }

        @Override
        public <S extends Slot> S compute(
                SlotMapOwner container, Object key, int index, SlotComputer<S> compute) {
            var map = new SingleSlotMap();
            container.replaceMap(map);
            return map.compute(container, key, index, compute);
        }
    }

    private static final class Iter implements Iterator<Slot> {
        private Slot next;

        Iter(Slot slot) {
            next = slot;
        }

        @Override
        public boolean hasNext() {
            return next != null;
        }

        @Override
        public Slot next() {
            Slot ret = next;
            if (ret == null) {
                throw new NoSuchElementException();
            }
            next = next.orderedNext;
            return ret;
        }
    }

    static final class SingleSlotMap implements SlotMap {

        SingleSlotMap() {}

        private Slot slot;

        @Override
        public Iterator<Slot> iterator() {
            if (slot == null) {
                return Collections.emptyIterator();
            } else {
                return new Iter(slot);
            }
        }

        @Override
        public int size() {
            return slot == null ? 0 : 1;
        }

        @Override
        public boolean isEmpty() {
            return slot == null;
        }

        @Override
        public Slot modify(SlotMapOwner owner, Object key, int index, int attributes) {
            final int indexOrHash = (key != null ? key.hashCode() : index);

            if (slot != null) {
                if (indexOrHash == slot.indexOrHash && Objects.equals(slot.name, key)) {
                    return slot;
                }
            }
            Slot newSlot = new Slot(key, index, attributes);
            add(owner, newSlot);
            return newSlot;
        }

        @Override
        public Slot query(Object key, int index) {
            final int indexOrHash = (key != null ? key.hashCode() : index);

            if (slot != null) {
                if (indexOrHash == slot.indexOrHash && Objects.equals(slot.name, key)) {
                    return slot;
                }
            }
            return null;
        }

        @Override
        public void add(SlotMapOwner owner, Slot newSlot) {
            if (slot == null) {
                slot = newSlot;
            } else if (owner == null) {
                throw new IllegalStateException();
            } else {
                var newMap = new EmbeddedSlotMap();
                owner.replaceMap(newMap);
                newMap.add(owner, slot);
                newMap.add(owner, newSlot);
            }
        }

        @Override
        public <S extends Slot> S compute(
                SlotMapOwner owner, Object key, int index, SlotComputer<S> c) {
            final int indexOrHash = (key != null ? key.hashCode() : index);

            if (slot != null) {
                if (indexOrHash == slot.indexOrHash && Objects.equals(slot.name, key)) {
                    S newSlot = c.compute(key, index, slot);
                    slot = newSlot;
                    return newSlot;
                }
                var newMap = new EmbeddedSlotMap();
                owner.replaceMap(newMap);
                newMap.add(owner, slot);
                return newMap.compute(owner, key, index, c);
            }
            S newSlot = c.compute(key, index, slot);
            slot = newSlot;
            return newSlot;
        }
    }

    static SlotMap EMPTY_SLOT_MAP = new EmptySlotMap();

    protected SlotMap map;

    SlotMapContainer() {
        this(DEFAULT_SIZE);
    }

    SlotMapContainer(int initialSize) {
        if (initialSize == 0) {
            map = EMPTY_SLOT_MAP;
        } else if (initialSize > LARGE_HASH_SIZE) {
            map = new HashSlotMap();
        } else {
            map = new EmbeddedSlotMap();
        }
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public int dirtySize() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public Slot modify(SlotMapOwner owner, Object key, int index, int attributes) {
        return map.modify(this, key, index, attributes);
    }

    @Override
    public <S extends Slot> S compute(
            SlotMapOwner owner, Object key, int index, SlotComputer<S> c) {
        return map.compute(this, key, index, c);
    }

    @Override
    public Slot query(Object key, int index) {
        return map.query(key, index);
    }

    @Override
    public void add(SlotMapOwner owner, Slot newSlot) {
        map.add(this, newSlot);
    }

    @Override
    public Iterator<Slot> iterator() {
        return map.iterator();
    }

    @Override
    public long readLock() {
        // No locking in the default implementation
        return 0L;
    }

    @Override
    public void unlockRead(long stamp) {
        // No locking in the default implementation
    }

    @Override
    public void replaceMap(SlotMap newMap) {
        map = newMap;
    }

    /**
     * Before inserting a new item in the map, check and see if we need to expand from the embedded
     * map to a HashMap that is more robust against large numbers of hash collisions.
     */
    protected void checkMapSize() {
        if (map == EMPTY_SLOT_MAP) {
            map = new EmbeddedSlotMap();
        } else if ((map instanceof EmbeddedSlotMap) && map.size() >= LARGE_HASH_SIZE) {
            SlotMap newMap = new HashSlotMap(map);
            map = newMap;
        }
    }
}
