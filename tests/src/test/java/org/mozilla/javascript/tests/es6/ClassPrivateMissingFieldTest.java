/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests.es6;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.testutils.Utils;

/** Accessing a private name on an object that does not have it is a TypeError. */
public class ClassPrivateMissingFieldTest {

    @Test
    public void readMissingPrivateField() {
        Utils.assertEcmaErrorES6(
                "TypeError: Cannot access private name Symbol(#x) on an object that does not have it. (test#1)",
                "class A { #x = 1; static r(o) { return o.#x; } }; A.r({})");
    }

    @Test
    public void presentPrivateFieldStillWorks() {
        Utils.assertWithAllModes_ES6(
                3, "class A { #x = 1; m() { this.#x = 3; return this.#x; } }; new A().m()");
    }
}
