/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.io.Serial;
import java.io.Serializable;

/**
 * Class instances represent serializable tags to mark special Object values.
 *
 * <p>Compatibility note: under jdk 1.1 use org.mozilla.javascript.serialize.ScriptableInputStream
 * to read serialized instances of UniqueTag as under this JDK version the default ObjectInputStream
 * would not restore them correctly as it lacks support for readResolve method
 */
public final class UniqueTag implements Serializable {
    @Serial private static final long serialVersionUID = -4320556826714577259L;

    private static final int ID_NOT_FOUND = 1;
    private static final int ID_NULL_VALUE = 2;
    private static final int ID_DOUBLE_MARK = 3;
    private static final int ID_COMPUTED_KEY = 4;
    private static final int ID_COMPUTED_METHOD = 5;

    /** Tag to mark non-existing values. */
    public static final UniqueTag NOT_FOUND = new UniqueTag(ID_NOT_FOUND);

    /** Tag to distinguish between uninitialized and null values. */
    public static final UniqueTag NULL_VALUE = new UniqueTag(ID_NULL_VALUE);

    /**
     * Tag to indicate that a object represents "double" with the real value stored somewhere else.
     */
    public static final UniqueTag DOUBLE_MARK = new UniqueTag(ID_DOUBLE_MARK);

    /** Tag to indicate that a object represents a computed key in an object literal. */
    public static final UniqueTag COMPUTED_KEY = new UniqueTag(ID_COMPUTED_KEY);

    /** Tag to indicate that a object represents a computed method key in an object literal. */
    public static final UniqueTag COMPUTED_METHOD = new UniqueTag(ID_COMPUTED_METHOD);

    private final int tagId;

    private UniqueTag(int tagId) {
        this.tagId = tagId;
    }

    public Object readResolve() {
        return switch (tagId) {
            case ID_NOT_FOUND -> NOT_FOUND;
            case ID_NULL_VALUE -> NULL_VALUE;
            case ID_DOUBLE_MARK -> DOUBLE_MARK;
            case ID_COMPUTED_KEY -> COMPUTED_KEY;
            case ID_COMPUTED_METHOD -> COMPUTED_METHOD;
            default -> throw new IllegalStateException(String.valueOf(tagId));
        };
    }

    // Overridden for better debug printouts
    @Override
    public String toString() {
        String name;
        switch (tagId) {
            case ID_NOT_FOUND:
                name = "NOT_FOUND";
                break;
            case ID_NULL_VALUE:
                name = "NULL_VALUE";
                break;
            case ID_DOUBLE_MARK:
                name = "DOUBLE_MARK";
                break;
            default:
                throw Kit.codeBug();
        }
        return super.toString() + ": " + name;
    }
}
