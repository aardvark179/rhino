/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests.es6;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.testutils.Utils;

/** Deleting a private field reference is an early error. */
public class ClassDeletePrivateTest {

    @Test
    public void deletePrivateField() {
        Utils.assertEvaluatorExceptionES6(
                "Private fields can not be deleted. (test#1)",
                "class A { #x = 1; m() { delete this.#x; } }");
    }

    @Test
    public void deleteParenthesizedPrivateField() {
        Utils.assertEvaluatorExceptionES6(
                "Private fields can not be deleted. (test#1)",
                "class A { #x = 1; m() { delete ((this.#x)); } }");
    }

    @Test
    public void deletePublicFieldStillWorks() {
        Utils.assertWithAllModes_ES6(
                true, "class A { x = 1; m() { return delete this.x; } }; new A().m()");
    }
}
