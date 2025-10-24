/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

// API class

package org.mozilla.javascript;

/**
 * Interface the common route of JS objects and environment records, i.e. a thing that has
 * properties and some form of ancestor.
 */
public interface PropHolder<T extends PropHolder<T>> {

    public Object get(Symbol key, T start);

    public Object get(String name, T start);

    public Object get(int index, T start);

    public boolean has(Symbol name, T start);

    public boolean has(String name, T start);

    public boolean has(int index, T start);

    public boolean put(Symbol name, T start, Object value);

    public boolean put(String name, T start, Object value);

    public boolean put(int index, T start, Object value);

    public boolean delete(Symbol name);

    public boolean delete(String name);

    public boolean delete(int index);

    /**
     * Get the parent scope of the object.
     *
     * @return the parent scope
     */
    public T getAncestor();
}
