/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.testutils.Utils;

public class AsyncArrowArgumentsTest {

    @Test
    public void asyncArrowUsesEnclosingArguments() {
        String script =
                "var y = [];\n"
                        + "function asyncFn(x) {\n"
                        + "  let a = arguments;\n"
                        + "  y.push(a);\n"
                        + "  y.push(arguments);\n"
                        + "  return async () => { y.push(a); y.push(arguments); };\n"
                        + "}\n"
                        + "asyncFn()();\n"
                        + "'' + (y[0] === y[1]) + (y[0] === y[2]) + (y[0] === y[3]);";
        Utils.assertWithAllModes_ES6("truetruetrue", script);
    }

    @Test
    public void asyncArrowSeesEnclosingArgumentValues() {
        String script =
                "var r;\n"
                        + "function f() { return async () => { r = arguments.length + ':' + arguments[1]; }; }\n"
                        + "f(1, 2)();\n"
                        + "r;";
        Utils.assertWithAllModes_ES6("2:2", script);
    }
}
