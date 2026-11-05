/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests.es6;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.testutils.Utils;

/** Tests for async methods in classes. */
public class ClassAsyncMethodsTest {

    private static final String AWAIT_ERROR =
            "\"await\" may not be used as an identifier name in an async function.";

    @Test
    public void awaitAsIdentifierInAsyncMethod() {
        Utils.assertEvaluatorExceptionES6(AWAIT_ERROR, "class C { async bob() { var await; } }");
    }

    @Test
    public void awaitAsIdentifierInStaticAsyncMethod() {
        Utils.assertEvaluatorExceptionES6(
                AWAIT_ERROR, "class C { static async bob() { var await; } }");
    }

    @Test
    public void awaitAsIdentifierInComputedAsyncMethod() {
        Utils.assertEvaluatorExceptionES6(
                AWAIT_ERROR, "class C { async ['bob']() { var await; } }");
    }

    @Test
    public void awaitAsIdentifierInAsyncGeneratorMethod() {
        Utils.assertEvaluatorExceptionES6(
                AWAIT_ERROR, "var C = class { static async *gen() { var await; } };");
        Utils.assertEvaluatorExceptionES6(
                AWAIT_ERROR, "var C = class { async *gen() { var await; } };");
    }

    @Test
    public void asyncMethodIsAsync() {
        Utils.assertWithAllModes_ES6(
                true, "class C { async bob() { return 1; } } new C().bob() instanceof Promise");
    }

    @Test
    public void membersNamedAsync() {
        Utils.assertWithAllModes_ES6(
                "m,s",
                "class C { async() { return 'm'; } static async() { return 's'; } }\n"
                        + "[new C().async(), C.async()].join()");
    }

    @Test
    public void fieldNamedAsyncBeforeNewline() {
        Utils.assertWithAllModes_ES6(
                "a,f",
                "class C { async\n f = 'f'; constructor() { this.async = 'a'; } }\n"
                        + "var c = new C(); [c.async, c.f].join()");
    }
}
