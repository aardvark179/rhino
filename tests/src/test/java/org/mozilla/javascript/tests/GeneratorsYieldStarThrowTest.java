/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import org.junit.jupiter.api.Test;
import org.mozilla.javascript.testutils.Utils;

/** Tests for calling throw() on a generator suspended in a yield* delegation. */
public class GeneratorsYieldStarThrowTest {

    private static String run(String iterator) {
        return Utils.lines(
                "var log = [];",
                "var iterable = {};",
                "iterable[Symbol.iterator] = function() {",
                "  return " + iterator + ";",
                "};",
                "function* g() {",
                "  try {",
                "    var r = yield* iterable;",
                "    log.push('result:' + r);",
                "  } catch (e) {",
                "    log.push('caught:' + (e instanceof TypeError ? 'TypeError' : e));",
                "  }",
                "}",
                "var iter = g();",
                "iter.next();",
                "var res = iter.throw('thrown');",
                "log.push('done:' + res.done);",
                "log.join(',');");
    }

    @Test
    public void missingThrowReturnThrows() {
        Utils.assertWithAllModes_ES6(
                "return,caught:87,done:true",
                run(
                        "{ next: function() { return { done: false }; },"
                                + " return: function() { log.push('return'); throw 87; } }"));
    }

    @Test
    public void missingThrowReturnGetterThrows() {
        Utils.assertWithAllModes_ES6(
                "caught:87,done:true",
                run(
                        "{ next: function() { return { done: false }; },"
                                + " get return() { throw 87; } }"));
    }

    @Test
    public void missingThrowReturnNotCallable() {
        Utils.assertWithAllModes_ES6(
                "caught:TypeError,done:true",
                run("{ next: function() { return { done: false }; }, return: 1 }"));
    }

    @Test
    public void missingThrowReturnResultNotObject() {
        Utils.assertWithAllModes_ES6(
                "return,caught:TypeError,done:true",
                run(
                        "{ next: function() { return { done: false }; },"
                                + " return: function() { log.push('return'); return 1; } }"));
    }

    @Test
    public void missingThrowReturnSucceeds() {
        Utils.assertWithAllModes_ES6(
                "return,caught:TypeError,done:true",
                run(
                        "{ next: function() { return { done: false }; },"
                                + " return: function() { log.push('return'); return {}; } }"));
    }

    @Test
    public void missingThrowAndReturn() {
        Utils.assertWithAllModes_ES6(
                "caught:TypeError,done:true",
                run("{ next: function() { return { done: false }; } }"));
    }

    @Test
    public void throwThrowsDoesNotCallReturn() {
        Utils.assertWithAllModes_ES6(
                "throw:thrown,caught:42,done:true",
                run(
                        "{ next: function() { return { done: false }; },"
                                + " throw: function(v) { log.push('throw:' + v); throw 42; },"
                                + " return: function() { log.push('return'); return {}; } }"));
    }

    @Test
    public void throwDoneDoesNotCallReturn() {
        Utils.assertWithAllModes_ES6(
                "throw:thrown,result:7,done:true",
                run(
                        "{ next: function() { return { done: false }; },"
                                + " throw: function(v) { log.push('throw:' + v);"
                                + " return { done: true, value: 7 }; },"
                                + " return: function() { log.push('return'); return {}; } }"));
    }

    @Test
    public void throwResultNotObject() {
        Utils.assertWithAllModes_ES6(
                "throw:thrown,caught:TypeError,done:true",
                run(
                        "{ next: function() { return { done: false }; },"
                                + " throw: function(v) { log.push('throw:' + v); return 1; },"
                                + " return: function() { log.push('return'); return {}; } }"));
    }

    @Test
    public void throwNotDoneContinuesDelegation() {
        Utils.assertWithAllModes_ES6(
                "throw:thrown,done:false",
                run(
                        "{ next: function() { return { done: false }; },"
                                + " throw: function(v) { log.push('throw:' + v);"
                                + " return { done: false, value: 3 }; } }"));
    }
}
