package org.mozilla.javascript.optimizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mozilla.javascript.optimizer.ExceptionManager.ECMAERROR_EXCEPTION;
import static org.mozilla.javascript.optimizer.ExceptionManager.EVALUATOR_EXCEPTION;
import static org.mozilla.javascript.optimizer.ExceptionManager.EXCEPTION_MAX;
import static org.mozilla.javascript.optimizer.ExceptionManager.FINALLY_EXCEPTION;
import static org.mozilla.javascript.optimizer.ExceptionManager.JAVASCRIPT_EXCEPTION;
import static org.mozilla.javascript.optimizer.ExceptionManager.THROWABLE_EXCEPTION;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mozilla.classfile.ByteCode;
import org.mozilla.classfile.ClassFileWriter;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Node;
import org.mozilla.javascript.Token;
import org.mozilla.javascript.optimizer.ExceptionTableReader.Entry;

class ExceptionManagerTest {

    private static final String JS = "org/mozilla/javascript/JavaScriptException";
    private static final String EVAL = "org/mozilla/javascript/EvaluatorException";
    private static final String ECMA = "org/mozilla/javascript/EcmaError";
    private static final String THROWABLE = "java/lang/Throwable";
    private static final String[] CATCH_TYPES = {JS, EVAL, ECMA};

    /**
     * Emits a method shaped like the code {@link BodyCodegen} generates, where each instruction is
     * a single byte so offsets are easy to predict.
     */
    private static final class Method {
        final ClassFileWriter cfw = new ClassFileWriter("Test", "java/lang/Object", "Test.js");
        final ExceptionManager manager = new ExceptionManager(cfw);
        final List<Integer> handlers = new ArrayList<>();
        final Map<Integer, String> handlerNames = new HashMap<>();
        final Map<String, Integer> handlerPCs = new HashMap<>();

        Method() {
            cfw.startMethod(
                    "test",
                    "(Ljava/lang/Object;)V",
                    (short) (ClassFileWriter.ACC_PUBLIC | ClassFileWriter.ACC_STATIC));
        }

        void code(int length) {
            for (int i = 0; i < length; i++) {
                cfw.add(ByteCode.NOP);
            }
        }

        int pc() {
            return cfw.getCurrentCodeOffset();
        }

        /** Push a try block, naming its handlers with the given prefix. */
        int[] pushTry(String name, Node finallyBlock, boolean hasCatch) {
            int[] labels = new int[EXCEPTION_MAX];
            if (hasCatch) {
                labels[JAVASCRIPT_EXCEPTION] = handler(name + " js");
                labels[EVALUATOR_EXCEPTION] = handler(name + " eval");
                labels[ECMAERROR_EXCEPTION] = handler(name + " ecma");
            }
            if (finallyBlock != null) {
                labels[FINALLY_EXCEPTION] = handler(name + " finally");
            }
            int start = cfw.acquireLabel();
            cfw.markLabel(start);
            manager.pushTry(finallyBlock, start, labels);
            return labels;
        }

        int handler(String name) {
            int label = cfw.acquireLabel();
            handlers.add(label);
            handlerNames.put(label, name);
            return label;
        }

        /** Close handlers at a label marked immediately afterwards, as for a catch target. */
        void endHandlers(int... types) {
            int label = cfw.acquireLabel();
            manager.endHandlers(label, types);
            cfw.markLabel(label);
        }

        void endCatch() {
            endHandlers(
                    JAVASCRIPT_EXCEPTION,
                    EVALUATOR_EXCEPTION,
                    ECMAERROR_EXCEPTION,
                    THROWABLE_EXCEPTION);
        }

        void endFinally() {
            endHandlers(FINALLY_EXCEPTION);
        }

        /** Emit an inlined copy of a finally block containing {@code body}. */
        void inline(Node finallyBlock, Runnable body) {
            int start = cfw.acquireLabel();
            int end = cfw.acquireLabel();
            cfw.markLabel(start);
            manager.enterInlinedFinally(finallyBlock, start);
            body.run();
            manager.exitInlinedFinally(finallyBlock, end);
            cfw.markLabel(end);
        }

        void inline(Node finallyBlock, int length) {
            inline(finallyBlock, () -> code(length));
        }

        List<Entry> finish() {
            cfw.add(ByteCode.RETURN);
            for (int label : handlers) {
                cfw.markHandler(label);
                handlerPCs.put(handlerNames.get(label), cfw.getCurrentCodeOffset());
                cfw.add(ByteCode.ATHROW);
            }
            manager.flush();
            cfw.stopMethod((short) 1);
            List<Entry> entries = ExceptionTableReader.read(cfw.toByteArray()).get("test");
            for (Entry e : entries) {
                assertTrue(e.start() < e.end(), "empty range " + e);
                assertFalse(e.covers(e.handler()), "handler covers itself " + e);
            }
            return entries;
        }

        Entry entry(int start, int end, String handler, String type) {
            return new Entry(start, end, handlerPCs.get(handler), type);
        }

        /** Entries for the JS, evaluator and ECMA error catches of a try, in table order. */
        List<Entry> catches(String name, int... ranges) {
            List<Entry> result = new ArrayList<>();
            String[] suffixes = {" js", " eval", " ecma"};
            for (int t = 0; t < CATCH_TYPES.length; t++) {
                for (int i = 0; i < ranges.length; i += 2) {
                    result.add(entry(ranges[i], ranges[i + 1], name + suffixes[t], CATCH_TYPES[t]));
                }
            }
            return result;
        }

        List<Entry> finallys(String name, int... ranges) {
            List<Entry> result = new ArrayList<>();
            for (int i = 0; i < ranges.length; i += 2) {
                result.add(entry(ranges[i], ranges[i + 1], name + " finally", null));
            }
            return result;
        }
    }

    private static Node finallyBlock() {
        return new Node(Token.FINALLY);
    }

    @SafeVarargs
    private static List<Entry> concat(List<Entry>... lists) {
        List<Entry> result = new ArrayList<>();
        for (List<Entry> l : lists) {
            result.addAll(l);
        }
        return result;
    }

    @Test
    void tryCatch() {
        Method m = new Method();
        m.pushTry("t", null, true);
        m.code(3);
        m.endCatch();
        m.code(2);
        m.manager.popTry();

        List<Entry> entries = m.finish();
        assertEquals(m.catches("t", 0, 3), entries);
    }

    @Test
    void throwableCatchComesAfterOtherCatches() {
        Method m = new Method();
        int[] labels = new int[EXCEPTION_MAX];
        labels[THROWABLE_EXCEPTION] = m.handler("throwable");
        labels[JAVASCRIPT_EXCEPTION] = m.handler("js");
        int start = m.cfw.acquireLabel();
        m.cfw.markLabel(start);
        m.manager.pushTry(null, start, labels);
        m.code(2);
        m.endCatch();
        m.manager.popTry();

        List<Entry> entries = m.finish();
        assertEquals(
                List.of(m.entry(0, 2, "js", JS), m.entry(0, 2, "throwable", THROWABLE)), entries);
    }

    @Test
    void inlinedFinallyIsExcludedFromEveryHandler() {
        // try { a; return; } catch (e) { b; return; } finally { f }
        Method m = new Method();
        Node f = finallyBlock();
        m.pushTry("t", f, true);
        m.code(2);
        m.inline(f, 3); // 2..5
        m.code(1);
        m.endCatch(); // 6
        m.code(2);
        m.inline(f, 3); // 8..11
        m.endFinally(); // 11
        m.manager.popTry();

        List<Entry> entries = m.finish();
        assertEquals(concat(m.catches("t", 0, 2, 5, 6), m.finallys("t", 0, 2, 5, 8)), entries);
    }

    @Test
    void tryFinallyWithoutCatch() {
        // try { a; break; b } finally { f }
        Method m = new Method();
        Node f = finallyBlock();
        m.pushTry("t", f, false);
        m.code(1);
        m.inline(f, 2); // 1..3
        m.code(1);
        m.endFinally(); // 4
        m.manager.popTry();

        List<Entry> entries = m.finish();
        assertEquals(m.finallys("t", 0, 1, 3, 4), entries);
    }

    @Test
    void exceptionPathCopyOfFinallyIsCoveredByOuterTry() {
        // try { try { a } finally { f } } catch (e) {}
        // where the inner finally handler rethrows after its own copy of f.
        Method m = new Method();
        Node f = finallyBlock();
        m.pushTry("outer", null, true);
        m.pushTry("inner", f, false);
        m.code(2);
        m.inline(f, 1); // normal completion 2..3
        m.endFinally(); // 3
        m.code(1);
        m.inline(f, 2); // 4..6, the exception path copy
        m.code(1);
        m.manager.popTry();
        m.endCatch(); // 7
        m.manager.popTry();

        List<Entry> entries = m.finish();
        assertEquals(concat(m.finallys("inner", 0, 2), m.catches("outer", 0, 7)), entries);
    }

    @Test
    void nestedReturnExcludesEachTryFromTheFinallysItLeaves() {
        // try {
        //   try { a; return; } catch (e) { b } finally { f2 }
        // } catch (e) { c } finally { f1 }
        Method m = new Method();
        Node f1 = finallyBlock();
        Node f2 = finallyBlock();
        m.pushTry("outer", f1, true);
        m.pushTry("inner", f2, true);
        m.code(1);
        m.inline(f2, 2); // 1..3, still covered by outer
        m.inline(f1, 2); // 3..5, covered by neither
        m.code(1);
        m.endCatch(); // 6
        m.code(1);
        m.inline(f2, 2); // 7..9
        m.endFinally(); // 9
        m.manager.popTry();
        m.code(1);
        m.endCatch(); // 10
        m.code(1);
        m.inline(f1, 2); // 11..13
        m.endFinally(); // 13
        m.manager.popTry();

        List<Entry> entries = m.finish();
        assertEquals(
                concat(
                        m.catches("inner", 0, 1, 5, 6),
                        m.finallys("inner", 0, 1, 5, 7),
                        m.catches("outer", 0, 3, 5, 10),
                        m.finallys("outer", 0, 3, 5, 11)),
                entries);
    }

    @Test
    void innerTryResumesAfterOnlyOuterFinallyIsInlined() {
        // try { try { a; break; b } catch (e) {} } finally { f }
        Method m = new Method();
        Node f = finallyBlock();
        m.pushTry("outer", f, false);
        m.pushTry("inner", null, true);
        m.code(1);
        m.inline(f, 2); // 1..3
        m.code(1);
        m.endCatch(); // 4
        m.manager.popTry();
        m.code(1);
        m.endFinally(); // 5
        m.manager.popTry();

        List<Entry> entries = m.finish();
        assertEquals(
                concat(m.catches("inner", 0, 1, 3, 4), m.finallys("outer", 0, 1, 3, 5)), entries);
    }

    @Test
    void tryInsideInlinedFinallyKeepsItsHandlers() {
        // try { a; return; } finally { try { x } catch (e) {} }
        Method m = new Method();
        Node f = finallyBlock();
        m.pushTry("outer", f, false);
        m.code(1);
        m.inline(
                f,
                () -> {
                    m.pushTry("nested", null, true); // 1
                    m.code(2);
                    m.endCatch(); // 3
                    m.code(1);
                    m.manager.popTry();
                }); // 1..4
        m.code(1);
        m.endFinally(); // 5
        m.manager.popTry();

        List<Entry> entries = m.finish();
        assertEquals(concat(m.catches("nested", 1, 3), m.finallys("outer", 0, 1, 4, 5)), entries);
    }

    @Test
    void returnFromTryInsideInlinedFinally() {
        // try {
        //   try { return; } finally { try { return; } finally { g } }
        // } catch (e) {}
        // The return inside f only leaves the nested try, so only g is inlined there.
        Method m = new Method();
        Node f = finallyBlock();
        Node g = finallyBlock();
        m.pushTry("outer", null, true);
        m.pushTry("inner", f, false);
        m.inline(
                f,
                () -> {
                    m.pushTry("nested", g, false);
                    m.code(1); // 0
                    m.inline(g, 2); // 1..3
                    m.endFinally(); // 3
                    m.manager.popTry();
                }); // 0..3
        m.code(1);
        m.endFinally(); // 4
        m.manager.popTry();
        m.code(1);
        m.endCatch(); // 5
        m.manager.popTry();

        List<Entry> entries = m.finish();
        assertEquals(
                concat(
                        m.finallys("nested", 0, 1),
                        m.finallys("inner", 3, 4),
                        m.catches("outer", 0, 5)),
                entries);
    }

    @Test
    void threeLevelsWithBreakThroughAllFinallys() {
        // try {
        //   try {
        //     try { a; break; } finally { f3 }
        //   } catch (e) {} finally { f2 }
        // } catch (e) {} finally { f1 }
        Method m = new Method();
        Node f1 = finallyBlock();
        Node f2 = finallyBlock();
        Node f3 = finallyBlock();
        m.pushTry("t1", f1, true);
        m.pushTry("t2", f2, true);
        m.pushTry("t3", f3, false);
        m.code(1);
        m.inline(f3, 2); // 1..3
        m.inline(f2, 2); // 3..5
        m.inline(f1, 2); // 5..7
        m.code(1);
        m.endFinally(); // 8
        m.manager.popTry();
        m.endCatch(); // 8
        m.code(1);
        m.endFinally(); // 9
        m.manager.popTry();
        m.endCatch(); // 9
        m.code(1);
        m.endFinally(); // 10
        m.manager.popTry();

        List<Entry> entries = m.finish();
        assertEquals(
                concat(
                        m.finallys("t3", 0, 1, 7, 8),
                        m.catches("t2", 0, 3, 7, 8),
                        m.finallys("t2", 0, 3, 7, 9),
                        m.catches("t1", 0, 5, 7, 9),
                        m.finallys("t1", 0, 5, 7, 10)),
                entries);
    }

    @Test
    void generatorStyleNestedTryKeepsOuterCatch() {
        // Generators emit finally blocks in place rather than inlining them.
        // try { try { a } finally { f } } catch (e) {}
        Method m = new Method();
        m.pushTry("outer", null, true);
        m.pushTry("inner", finallyBlock(), false);
        m.code(2);
        m.endFinally(); // 2
        m.code(2);
        m.manager.popTry();
        m.code(1);
        m.endCatch(); // 5
        m.manager.popTry();

        List<Entry> entries = m.finish();
        assertEquals(concat(m.finallys("inner", 0, 2), m.catches("outer", 0, 5)), entries);
    }

    @Test
    void catchAllComesLast() {
        Method m = new Method();
        int all = m.handler("all");
        int start = m.cfw.acquireLabel();
        int end = m.cfw.acquireLabel();
        m.cfw.markLabel(start);
        m.manager.addCatchAll(start, end, all);
        m.pushTry("t", null, true);
        m.code(1);
        m.endCatch(); // 1
        m.manager.popTry();
        m.code(1);
        m.cfw.markLabel(end);

        List<Entry> entries = m.finish();
        assertEquals(concat(m.catches("t", 0, 1), List.of(m.entry(0, 2, "all", null))), entries);
    }

    @Test
    void unbalancedUseIsRejected() {
        Method m = new Method();
        m.pushTry("t", null, true);
        m.code(1);
        assertThrows(RuntimeException.class, () -> m.manager.popTry());

        Method m2 = new Method();
        m2.pushTry("t", null, false);
        assertThrows(RuntimeException.class, m2.manager::flush);

        Method m3 = new Method();
        m3.pushTry("t", finallyBlock(), false);
        assertThrows(
                RuntimeException.class,
                () -> m3.manager.enterInlinedFinally(finallyBlock(), m3.cfw.acquireLabel()));
    }

    @Test
    void compiledCatchTypesShareTheSameRanges() {
        String source =
                String.join(
                        "\n",
                        "function f(c) {",
                        "  try {",
                        "    try { if (c) return 1; g(); }",
                        "    catch (e) { return 2; }",
                        "    finally { g(); }",
                        "    for (;;) { try { break; } catch (e) { g(); } finally { g(); } }",
                        "  } catch (e) { g(); } finally { g(); }",
                        "}");
        CompilerEnvirons env = new CompilerEnvirons();
        Object[] classes = new ClassCompiler(env).compileToClassFiles(source, "test.js", 1, "T");
        int checked = 0;
        for (int i = 1; i < classes.length; i += 2) {
            Map<String, List<Entry>> methods = ExceptionTableReader.read((byte[]) classes[i]);
            for (Map.Entry<String, List<Entry>> method : methods.entrySet()) {
                if (!method.getKey().contains("_f_")) {
                    continue;
                }
                List<Entry> entries = method.getValue();
                for (Entry e : entries) {
                    assertFalse(e.covers(e.handler()), "handler covers itself " + e);
                }
                List<Entry> js = entries.stream().filter(e -> JS.equals(e.type())).toList();
                List<Entry> eval = entries.stream().filter(e -> EVAL.equals(e.type())).toList();
                List<Entry> ecma = entries.stream().filter(e -> ECMA.equals(e.type())).toList();
                assertEquals(ranges(js), ranges(eval), method.getKey());
                assertEquals(ranges(js), ranges(ecma), method.getKey());
                assertFalse(js.isEmpty(), method.getKey());
                checked++;
            }
        }
        assertEquals(1, checked);
    }

    private static List<String> ranges(List<Entry> entries) {
        return entries.stream().map(e -> e.start() + "-" + e.end()).toList();
    }
}
