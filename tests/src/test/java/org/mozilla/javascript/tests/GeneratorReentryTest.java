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
    @Disabled("Compiled mode saves the stack at a yield as Objects, so typed values fail to verify")
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
}
