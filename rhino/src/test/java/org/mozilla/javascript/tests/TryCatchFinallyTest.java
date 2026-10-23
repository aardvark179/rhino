package org.mozilla.javascript.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.LambdaFunction;
import org.mozilla.javascript.TopLevel;
import org.mozilla.javascript.testutils.Utils;

/**
 * Checks that the code paths through try/catch/finally blocks behave identically in all modes. In
 * compiled mode finally blocks are duplicated at each exit from a try, so these focus on exceptions
 * raised inside those copies.
 */
class TryCatchFinallyTest {

    private static final String[] THROWS = {
        "throw 'x'", "null.x", "throwEvaluator()",
    };

    private static void assertWithAllModes(String expected, String script) {
        assertWithAllModes(new ContextFactory(), expected, script);
    }

    private static void assertWithAllModes(ContextFactory factory, String expected, String script) {
        Utils.runWithAllModes(
                factory,
                cx -> {
                    cx.setLanguageVersion(Context.VERSION_ES6);
                    TopLevel scope = cx.initStandardObjects();
                    scope.put(
                            "throwEvaluator",
                            scope,
                            new LambdaFunction(
                                    scope,
                                    "throwEvaluator",
                                    0,
                                    (c, s, t, a) -> {
                                        throw Context.reportRuntimeError("evaluator");
                                    }));
                    Object result = cx.evaluateString(scope, script, "test.js", 1, null);
                    assertEquals(
                            expected, Context.toString(result), cx.getEvaluationMethod().name());
                    return null;
                });
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2})
    void throwInFinallyOnReturnIsNotCaughtBySameTry(int kind) {
        assertWithAllModes(
                "try,finally,outer",
                Utils.lines(
                        "var log = [];",
                        "function f() {",
                        "  try { log.push('try'); return 1; }",
                        "  catch (e) { log.push('catch'); }",
                        "  finally { log.push('finally'); " + THROWS[kind] + "; }",
                        "}",
                        "try { f(); } catch (e) { log.push('outer'); }",
                        "log.join()"));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2})
    void throwInFinallyOnBreakIsNotCaughtBySameTry(int kind) {
        assertWithAllModes(
                "try,finally,outer",
                Utils.lines(
                        "var log = [];",
                        "try {",
                        "  while (true) {",
                        "    try { log.push('try'); break; }",
                        "    catch (e) { log.push('catch'); }",
                        "    finally { log.push('finally'); " + THROWS[kind] + "; }",
                        "  }",
                        "} catch (e) { log.push('outer'); }",
                        "log.join()"));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2})
    void throwInFinallyOnContinueIsNotCaughtBySameTry(int kind) {
        assertWithAllModes(
                "try,finally,outer",
                Utils.lines(
                        "var log = [];",
                        "try {",
                        "  for (var i = 0; i < 2; i++) {",
                        "    try { log.push('try'); continue; }",
                        "    catch (e) { log.push('catch'); }",
                        "    finally { log.push('finally'); " + THROWS[kind] + "; }",
                        "  }",
                        "} catch (e) { log.push('outer'); }",
                        "log.join()"));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2})
    void throwInFinallyOnReturnFromCatchIsNotCaughtBySameTry(int kind) {
        assertWithAllModes(
                "try,catch,finally,outer",
                Utils.lines(
                        "var log = [];",
                        "function f() {",
                        "  try { log.push('try'); throw 1; }",
                        "  catch (e) { log.push('catch'); return 2; }",
                        "  finally { log.push('finally'); " + THROWS[kind] + "; }",
                        "}",
                        "try { f(); } catch (e) { log.push('outer'); }",
                        "log.join()"));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2})
    void throwBeforeReturnIsCaughtWithFinallyRunOnce(int kind) {
        assertWithAllModes(
                "catch,finally",
                Utils.lines(
                        "var log = [];",
                        "function f(c) {",
                        "  try { " + THROWS[kind] + "; if (c) return 1; }",
                        "  catch (e) { log.push('catch'); return 2; }",
                        "  finally { log.push('finally'); }",
                        "}",
                        "f(true);",
                        "log.join()"));
    }

    @Test
    void throwInFinallyWithEnhancedJavaAccess() {
        ContextFactory factory =
                new ContextFactory() {
                    @Override
                    protected boolean hasFeature(Context cx, int featureIndex) {
                        if (featureIndex == Context.FEATURE_ENHANCED_JAVA_ACCESS) {
                            return true;
                        }
                        return super.hasFeature(cx, featureIndex);
                    }
                };
        // Catching the exception in JS would wrap it as a Java object, so let it escape.
        Utils.runWithAllModes(
                factory,
                cx -> {
                    TopLevel scope = cx.initStandardObjects();
                    cx.evaluateString(
                            scope,
                            Utils.lines(
                                    "var log = [];",
                                    "function f() {",
                                    "  try { log.push('try'); return 1; }",
                                    "  catch (e) { log.push('catch'); }",
                                    "  finally { log.push('finally'); throw 'x'; }",
                                    "}"),
                            "test.js",
                            1,
                            null);
                    assertThrows(
                            JavaScriptException.class,
                            () -> cx.evaluateString(scope, "f()", "test.js", 1, null));
                    assertEquals(
                            "try,finally",
                            cx.evaluateString(scope, "log.join()", "test.js", 1, null),
                            cx.getEvaluationMethod().name());
                    return null;
                });
    }

    @Test
    void throwInCatchRunsFinallyAfterUnreachableReturnInTry() {
        assertWithAllModes(
                "catch ex1,finally,outer ex3",
                Utils.lines(
                        "var log = [];",
                        "function f() {",
                        "  try { throw 'ex1'; return 1; }",
                        "  catch (e) { log.push('catch ' + e); throw 'ex2'; }",
                        "  finally { log.push('finally'); throw 'ex3'; }",
                        "}",
                        "try { f(); } catch (e) { log.push('outer ' + e); }",
                        "log.join()"));
    }

    @Test
    void nestedInnerFinallyThrowIsCaughtByOuterCatch() {
        assertWithAllModes(
                "inner try,inner finally,outer catch a,outer finally,2",
                Utils.lines(
                        "var log = [];",
                        "function f() {",
                        "  try {",
                        "    try { log.push('inner try'); return 1; }",
                        "    catch (e) { log.push('inner catch ' + e); }",
                        "    finally { log.push('inner finally'); throw 'a'; }",
                        "  } catch (e) { log.push('outer catch ' + e); return 2; }",
                        "  finally { log.push('outer finally'); }",
                        "}",
                        "var r = f();",
                        "log.push(r);",
                        "log.join()"));
    }

    @Test
    void nestedOuterFinallyThrowOnReturnEscapesBothTries() {
        assertWithAllModes(
                "t,if,of,caller x",
                Utils.lines(
                        "var log = [];",
                        "function f() {",
                        "  try {",
                        "    try { log.push('t'); return 1; }",
                        "    catch (e) { log.push('ic'); }",
                        "    finally { log.push('if'); }",
                        "  } catch (e) { log.push('oc'); }",
                        "  finally { log.push('of'); throw 'x'; }",
                        "}",
                        "try { f(); } catch (e) { log.push('caller ' + e); }",
                        "log.join()"));
    }

    @Test
    void nestedInnerFinallyThrowOnReturnSkipsInnerCatch() {
        assertWithAllModes(
                "t,if,oc x,of",
                Utils.lines(
                        "var log = [];",
                        "function f() {",
                        "  try {",
                        "    try { log.push('t'); return 1; }",
                        "    catch (e) { log.push('ic'); }",
                        "    finally { log.push('if'); null.x; }",
                        "  } catch (e) { log.push('oc x'); }",
                        "  finally { log.push('of'); }",
                        "}",
                        "f();",
                        "log.join()"));
    }

    @Test
    void threeLevelsWithLabelledBreakThroughThrowingFinally() {
        assertWithAllModes(
                "f1,f2,c3 x,f3,f1,f2,c3 x,f3",
                Utils.lines(
                        "var log = [];",
                        "outer: for (var i = 0; i < 2; i++) {",
                        "  try {",
                        "    try {",
                        "      try { break outer; }",
                        "      finally { log.push('f1'); }",
                        "    } catch (e) { log.push('c2 ' + e); }",
                        "    finally { log.push('f2'); throw 'x'; }",
                        "  } catch (e) { log.push('c3 ' + e); }",
                        "  finally { log.push('f3'); }",
                        "}",
                        "log.join()"));
    }

    @Test
    void threeLevelsWithContinueThroughMiddleFinally() {
        assertWithAllModes(
                "t0,f1,f2,f3,t1,f1,f2,f3,done",
                Utils.lines(
                        "var log = [];",
                        "for (var i = 0; i < 2; i++) {",
                        "  try {",
                        "    try {",
                        "      try { log.push('t' + i); continue; }",
                        "      catch (e) { log.push('c1'); }",
                        "      finally { log.push('f1'); }",
                        "    } catch (e) { log.push('c2'); }",
                        "    finally { log.push('f2'); }",
                        "  } catch (e) { log.push('c3'); }",
                        "  finally { log.push('f3'); }",
                        "}",
                        "log.push('done');",
                        "log.join()"));
    }

    @Test
    void tryCatchInsideInlinedFinally() {
        assertWithAllModes(
                "ft,fc y,1",
                Utils.lines(
                        "var log = [];",
                        "function f() {",
                        "  try { return 1; }",
                        "  finally {",
                        "    try { log.push('ft'); throw 'y'; }",
                        "    catch (e) { log.push('fc ' + e); }",
                        "  }",
                        "}",
                        "log.push(f());",
                        "log.join()"));
    }

    @Test
    void tryFinallyInsideInlinedFinally() {
        assertWithAllModes(
                "ft,ff,oc z",
                Utils.lines(
                        "var log = [];",
                        "function f() {",
                        "  try {",
                        "    try { return 1; }",
                        "    catch (e) { log.push('ic ' + e); }",
                        "    finally {",
                        "      try { log.push('ft'); return 2; }",
                        "      finally { log.push('ff'); throw 'z'; }",
                        "    }",
                        "  } catch (e) { log.push('oc ' + e); }",
                        "}",
                        "f();",
                        "log.join()"));
    }

    @Test
    void generatorNestedTryFinallyKeepsOuterCatch() {
        assertWithAllModes(
                "if,oc boom,2",
                Utils.lines(
                        "var log = [];",
                        "function* g() {",
                        "  try {",
                        "    try { yield 1; }",
                        "    finally { log.push('if'); }",
                        "  } catch (e) { log.push('oc ' + e); yield 2; }",
                        "}",
                        "var it = g();",
                        "it.next();",
                        "log.push(it.throw('boom').value);",
                        "log.join()"));
    }

    @Test
    void generatorThrowInInnerFinallyIsCaughtByOuterCatch() {
        assertWithAllModes(
                "if,oc x,2,of",
                Utils.lines(
                        "var log = [];",
                        "function* g() {",
                        "  try {",
                        "    try { yield 1; }",
                        "    catch (e) { log.push('ic ' + e); }",
                        "    finally { log.push('if'); throw 'x'; }",
                        "  } catch (e) { log.push('oc ' + e); yield 2; }",
                        "  finally { log.push('of'); }",
                        "}",
                        "var it = g();",
                        "it.next();",
                        "log.push(it.next().value);",
                        "it.next();",
                        "log.join()"));
    }

    @Test
    void generatorReturnRunsNestedFinallysOnce() {
        assertWithAllModes(
                "f1,f2,f3,true",
                Utils.lines(
                        "var log = [];",
                        "function* g() {",
                        "  try {",
                        "    try {",
                        "      try { yield 1; }",
                        "      catch (e) { log.push('c1'); }",
                        "      finally { log.push('f1'); }",
                        "    } catch (e) { log.push('c2'); }",
                        "    finally { log.push('f2'); }",
                        "  } catch (e) { log.push('c3'); }",
                        "  finally { log.push('f3'); }",
                        "}",
                        "var it = g();",
                        "it.next();",
                        "log.push(it.return(5).done);",
                        "log.join()"));
    }
}
