package org.mozilla.javascript.tests;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.testutils.Utils;

/** Checks that generators resume at the right place, with the right locals restored. */
public class GeneratorReentryTest {

    @Test
    public void sequentialYieldsReceiveSentValues() {
        Utils.assertWithAllModes_ES6(
                "a,b,c,1|2|3",
                Utils.lines(
                        "function* g() {",
                        "  var log = [];",
                        "  var va = yield 'a';",
                        "  log.push(va);",
                        "  var vb = yield 'b';",
                        "  log.push(vb);",
                        "  var vc = yield 'c';",
                        "  log.push(vc);",
                        "  return log.join('|');",
                        "}",
                        "var it = g(), out = [], r = it.next();",
                        "while (!r.done) { out.push(r.value); r = it.next(out.length); }",
                        "out.push(r.value);",
                        "out.join(',')"));
    }

    @Test
    public void liveLocalsAreRestoredAcrossYields() {
        Utils.assertWithAllModes_ES6(
                "x:1,y:2,z:3",
                Utils.lines(
                        "function* g(o) {",
                        "  for (var k in o) {",
                        "    try {",
                        "      throw o[k];",
                        "    } catch (e) {",
                        "      yield k + ':' + e;",
                        "    }",
                        "  }",
                        "}",
                        "[...g({x: 1, y: 2, z: 3})].join(',')"));
    }

    @Test
    public void yieldInsideTryAndFinally() {
        Utils.assertWithAllModes_ES6(
                "t,f1,f2,done",
                Utils.lines(
                        "var log = [];",
                        "function* g() {",
                        "  try {",
                        "    yield 't';",
                        "  } finally {",
                        "    var vf1 = yield 'f1';",
                        "  log.push(vf1);",
                        "    yield 'f2';",
                        "  }",
                        "}",
                        "var out = [...g()];",
                        "out.push('done');",
                        "out.join(',')"));
    }

    @Test
    public void yieldInsideFinallyReachedByReturnAndContinue() {
        Utils.assertWithAllModes_ES6(
                "0,f0,1,f1,r",
                Utils.lines(
                        "function* g() {",
                        "  for (var i = 0; ; i++) {",
                        "    try {",
                        "      yield i;",
                        "      if (i == 1) return 'r';",
                        "      continue;",
                        "    } finally {",
                        "      yield 'f' + i;",
                        "    }",
                        "  }",
                        "}",
                        "var it = g(), out = [], r;",
                        "while (!(r = it.next()).done) out.push(r.value);",
                        "out.push(r.value);",
                        "out.join(',')"));
    }

    @Test
    public void returnAndThrowReenterAtYield() {
        Utils.assertWithAllModes_ES6(
                "1,caught:boom,2,cleanup,true",
                Utils.lines(
                        "var out = [];",
                        "function* g() {",
                        "  try {",
                        "    yield 1;",
                        "  } catch (e) {",
                        "    out.push('caught:' + e);",
                        "  }",
                        "  try {",
                        "    yield 2;",
                        "  } finally {",
                        "    out.push('cleanup');",
                        "  }",
                        "}",
                        "var it = g();",
                        "out.push(it.next().value);",
                        "out.push(it.throw('boom').value);",
                        "out.push(it.return(3).done);",
                        "out.join(',')"));
    }

    @Test
    public void nestedYieldsInLoop() {
        Utils.assertWithAllModes_ES6(
                "0,x0,1,x1,y0|y1,true",
                Utils.lines(
                        "function* g() {",
                        "  var r = [];",
                        "  for (var i = 0; i < 2; i++) { var v = yield (yield i); r.push(v); }",
                        "  return r.join('|');",
                        "}",
                        "var it = g(), out = [];",
                        "out.push(it.next().value);",
                        "out.push(it.next('x0').value);",
                        "out.push(it.next('y0').value);",
                        "out.push(it.next('x1').value);",
                        "var last = it.next('y1');",
                        "out.push(last.value, last.done);",
                        "out.join(',')"));
    }

    @Test
    public void yieldAsCallArgument() {
        Utils.assertWithAllModes_ES6(
                "1|2",
                Utils.lines(
                        "function* g() {",
                        "  var log = [];",
                        "  log.push((yield 'a'));",
                        "  log.push((yield 'b'));",
                        "  return log.join('|');",
                        "}",
                        "var it = g();",
                        "it.next();",
                        "it.next(1);",
                        "it.next(2).value"));
    }

    @Test
    public void yieldInsideNestedFinallyReachedByReturn() {
        Utils.assertWithAllModes_ES6(
                "0,f,o,r",
                Utils.lines(
                        "function* g() {",
                        "  try {",
                        "    try {",
                        "      yield 0;",
                        "      return 'r';",
                        "    } finally {",
                        "      yield 'f';",
                        "    }",
                        "  } finally {",
                        "    yield 'o';",
                        "  }",
                        "}",
                        "var it = g(), out = [], r;",
                        "while (!(r = it.next()).done) out.push(r.value);",
                        "out.push(r.value);",
                        "out.join(',')"));
    }

    @Test
    public void yieldAsCallArgumentInCatch() {
        Utils.assertWithAllModes_ES6(
                "e:1",
                Utils.lines(
                        "function* g() {",
                        "  try {",
                        "    throw 'e';",
                        "  } catch (e) {",
                        "    var log = [];",
                        "    log.push(e + ':' + (yield 'a'));",
                        "    return log.join();",
                        "  }",
                        "}",
                        "var it = g();",
                        "it.next();",
                        "it.next(1).value"));
    }

    @Test
    public void yieldAsCallArgumentInFinally() {
        Utils.assertWithAllModes_ES6(
                "f:1",
                Utils.lines(
                        "var log = [];",
                        "function* g() {",
                        "  try {",
                        "    yield 't';",
                        "  } finally {",
                        "    log.push('f:' + (yield 'f'));",
                        "  }",
                        "}",
                        "var it = g();",
                        "it.next();",
                        "it.next();",
                        "it.next(1);",
                        "log.join()"));
    }

    @Test
    public void yieldsAsArgumentsOfAMethodCall() {
        Utils.assertWithAllModes_ES6(
                "3",
                Utils.lines(
                        "function* g() {",
                        "  return Math.max((yield 1), (yield 2), 0);",
                        "}",
                        "var it = g();",
                        "it.next();",
                        "it.next(3);",
                        "String(it.next(2).value)"));
    }

    @Test
    public void yieldInCompoundAssignments() {
        Utils.assertWithAllModes_ES6(
                "3,0,5",
                Utils.lines(
                        "function* g() {",
                        "  var o = {x: 1};",
                        "  o.x += (yield 'a');",
                        "  var a = [0, 0, 0];",
                        "  a[(yield 'i')] = (yield 'v');",
                        "  return [o.x].concat(a.slice(1)).join();",
                        "}",
                        "var it = g();",
                        "it.next();",
                        "it.next(2);",
                        "it.next(2);",
                        "it.next(5).value"));
    }

    @Test
    public void typedTemporariesLiveAcrossYields() {
        // Exceptions, scopes and enumerators held in locals while suspended
        Utils.assertWithAllModes_ES6(
                "x,xy,a:f,F,done",
                Utils.lines(
                        "function* g() {",
                        "  try {",
                        "    try { throw 'x'; } catch (e) {",
                        "      yield e;",
                        "      try { throw 'y'; } catch (f) { yield e + f; }",
                        "    }",
                        "    for (var k in {a: 1}) {",
                        "      try { throw k; } catch (e) { yield e + ':' + (yield 'f'); }",
                        "    }",
                        "  } finally {",
                        "    yield 'F';",
                        "  }",
                        "  return 'done';",
                        "}",
                        "var it = g(), r, out = [], sent;",
                        "while (!(r = it.next(sent)).done) {",
                        "  if (r.value === 'f') { sent = 'f'; continue; }",
                        "  out.push(r.value); sent = undefined;",
                        "}",
                        "out.push(r.value);",
                        "out.join()"));
    }

    @Test
    @Disabled(
            "Compiled mode saves a local in the finally handler that is not set on every path into"
                    + " it")
    public void yieldInWithAndFinally() {
        Utils.assertWithAllModes_ES6(
                "1,F",
                Utils.lines(
                        "function* g() {",
                        "  try {",
                        "    var o = {p: 1};",
                        "    with (o) { yield p; }",
                        "  } finally {",
                        "    yield 'F';",
                        "  }",
                        "}",
                        "var out = [];",
                        "for (var v of g()) out.push(v);",
                        "out.join()"));
    }
}
