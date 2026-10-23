/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.testutils.Utils;

class GeneratorParamDestructuringTest {
    @Test
    void arrayPattern() {
        Utils.assertWithAllModes_ES6(
                1, "function* g([a]) { yield a; }\n" + "g([1, 2]).next().value");
    }

    @Test
    void elision() {
        Utils.assertWithAllModes_ES6(
                "done", "function* g([,]) { yield 'done'; }\n" + "g([1]).next().value");
    }

    @Test
    void arrayPatternWithDefault() {
        Utils.assertWithAllModes_ES6(
                2, "function* g([a] = [2]) { yield a; }\n" + "g().next().value");
    }

    @Test
    void arrayPatternNestedInObjectPattern() {
        Utils.assertWithAllModes_ES6(
                3, "function* g({x: [a]}) { yield a; }\n" + "g({x: [3]}).next().value");
    }

    @Test
    void generatorMethod() {
        Utils.assertWithAllModes_ES6(
                4, "var o = { *g([a, b]) { yield a + b; } };\n" + "o.g([1, 3]).next().value");
    }

    @Test
    void asyncGenerator() {
        Utils.assertWithAllModes_ES6(
                "x",
                "var log;\n" + "async function* g([a = (log = 'x')]) {}\n" + "g([]);\n" + "log");
    }

    @Test
    void iteratorClosedOnCompletion() {
        Utils.assertWithAllModes_ES6(
                1,
                "var closed = 0;\n"
                        + "var iterable = { [Symbol.iterator]() { return {\n"
                        + "  next() { return { value: 1, done: false }; },\n"
                        + "  return() { closed++; return {}; } }; } };\n"
                        + "function* g([a]) {}\n"
                        + "g(iterable);\n"
                        + "closed");
    }

    @Test
    void iteratorClosedOnInitializerThrow() {
        Utils.assertWithAllModes_ES6(
                "boom 1",
                "var closed = 0;\n"
                        + "var iterable = { [Symbol.iterator]() { return {\n"
                        + "  next() { return { value: undefined, done: false }; },\n"
                        + "  return() { closed++; return {}; } }; } };\n"
                        + "function* g([a = (() => { throw new Error('boom'); })()]) {}\n"
                        + "var res;\n"
                        + "try { g(iterable); } catch (e) { res = e.message + ' ' + closed; }\n"
                        + "res");
    }
}
