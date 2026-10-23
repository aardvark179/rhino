package org.mozilla.javascript.optimizer;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import org.mozilla.classfile.ClassFileWriter;
import org.mozilla.javascript.Kit;
import org.mozilla.javascript.Node;

/**
 * Builds the exception table for a method. All exception handler entries for compiled JavaScript
 * should be registered through this class rather than directly with the {@link ClassFileWriter}.
 *
 * <p>Every time a try block is encountered it should be pushed with {@link #pushTry}, which opens a
 * range for each of its handlers. {@link #endHandlers} closes ranges at the point the try block
 * stops covering them (the start of the catch or finally block), and {@link #popTry} removes the
 * try block once its code has been generated.
 *
 * <p>Outside generators finally blocks are duplicated at every exit from their try block. Those
 * copies must not be covered by the handlers of the try blocks they are leaving, so {@link
 * #enterInlinedFinally} suspends the handlers of every try block from the innermost out to the one
 * owning the finally block, and {@link #exitInlinedFinally} resumes them.
 *
 * <p>Entries are buffered and written by {@link #flush} with the most deeply nested handlers first,
 * because the JVM uses the first matching entry in the table.
 */
class ExceptionManager {
    static final int JAVASCRIPT_EXCEPTION = 0;
    static final int EVALUATOR_EXCEPTION = 1;
    static final int ECMAERROR_EXCEPTION = 2;
    static final int THROWABLE_EXCEPTION = 3;
    // Finally catch-alls are technically Throwable, but we want a distinction
    // here and we want to use a null string instead of an explicit Throwable
    // string.
    static final int FINALLY_EXCEPTION = 4;
    static final int EXCEPTION_MAX = 5;

    private final ClassFileWriter cfw;
    // A stack of try block information ordered by lexical scoping
    private final ArrayDeque<TryInfo> tries = new ArrayDeque<>();
    private final List<Entry> entries = new ArrayList<>();

    ExceptionManager(ClassFileWriter cfw) {
        this.cfw = cfw;
    }

    /**
     * Push a new try block and start covering code from {@code startLabel} with its handlers.
     *
     * @param finallyBlock the FINALLY node of the try block, or null if there is none
     * @param startLabel the label where the try block begins
     * @param handlerLabels the handler label for each exception type, or 0 if the type is not
     *     handled
     */
    void pushTry(Node finallyBlock, int startLabel, int[] handlerLabels) {
        if (handlerLabels.length != EXCEPTION_MAX) throw Kit.codeBug();
        TryInfo info = new TryInfo(finallyBlock, tries.size() + 1);
        for (int i = 0; i < EXCEPTION_MAX; i++) {
            if (handlerLabels[i] != 0) {
                info.handlerLabels[i] = handlerLabels[i];
                info.starts[i] = startLabel;
            }
        }
        tries.addLast(info);
    }

    /**
     * Stop covering code with the given handlers of the innermost try block.
     *
     * @param endLabel the label just after the last code that should be covered
     * @param exceptionTypes the exception types whose handlers should be closed
     */
    void endHandlers(int endLabel, int... exceptionTypes) {
        TryInfo top = tries.getLast();
        if (top.suspendedBy != null) throw Kit.codeBug();
        for (int type : exceptionTypes) {
            if (top.handlerLabels[type] != 0) {
                closeRange(top, type, endLabel);
                top.handlerLabels[type] = 0;
            }
        }
    }

    /** Remove the innermost try block, all of whose handlers must have been closed. */
    void popTry() {
        TryInfo top = tries.removeLast();
        if (top.suspendedBy != null) throw Kit.codeBug();
        for (int i = 0; i < EXCEPTION_MAX; i++) {
            if (top.handlerLabels[i] != 0) {
                throw Kit.codeBug("unclosed exception handler");
            }
        }
    }

    /**
     * Mark the start of an inlined copy of a finally block.
     *
     * <p>Every try block from the innermost out to the one owning the finally block that is not
     * already suspended stops covering code at {@code finallyStart}.
     *
     * @param finallyBlock the finally block that is being inlined
     * @param finallyStart the label of the beginning of the inlined code
     */
    void enterInlinedFinally(Node finallyBlock, int finallyStart) {
        Iterator<TryInfo> iter = tries.descendingIterator();
        while (iter.hasNext()) {
            TryInfo info = iter.next();
            if (info.suspendedBy == null) {
                for (int i = 0; i < EXCEPTION_MAX; i++) {
                    if (info.handlerLabels[i] != 0) {
                        closeRange(info, i, finallyStart);
                        info.starts[i] = 0;
                    }
                }
                info.suspendedBy = finallyBlock;
            }
            if (info.finallyBlock == finallyBlock) {
                return;
            }
        }
        throw Kit.codeBug("finally block is not on the try stack");
    }

    /**
     * Mark the end of an inlined copy of a finally block, resuming the handlers that {@link
     * #enterInlinedFinally} suspended for it.
     *
     * @param finallyBlock the finally block that is being inlined
     * @param finallyEnd the label of the end of the inlined code
     */
    void exitInlinedFinally(Node finallyBlock, int finallyEnd) {
        Iterator<TryInfo> iter = tries.descendingIterator();
        while (iter.hasNext()) {
            TryInfo info = iter.next();
            if (info.suspendedBy == finallyBlock) {
                for (int i = 0; i < EXCEPTION_MAX; i++) {
                    if (info.handlerLabels[i] != 0) {
                        info.starts[i] = finallyEnd;
                    }
                }
                info.suspendedBy = null;
            }
            if (info.finallyBlock == finallyBlock) {
                return;
            }
        }
        throw Kit.codeBug("finally block is not on the try stack");
    }

    /**
     * Register a catch-all handler outside of any try block. It will be placed after all try block
     * handlers in the exception table.
     */
    void addCatchAll(int startLabel, int endLabel, int handlerLabel) {
        entries.add(new Entry(startLabel, endLabel, handlerLabel, FINALLY_EXCEPTION, 0));
    }

    /** Write all buffered entries to the class file. Must be called before the method is ended. */
    void flush() {
        if (!tries.isEmpty()) throw Kit.codeBug("unbalanced try blocks");
        // Within one try block the more general types must come last
        entries.sort(Comparator.comparingInt((Entry e) -> -e.depth).thenComparingInt(e -> e.type));
        for (Entry e : entries) {
            if (cfw.getLabelPC(e.start) != cfw.getLabelPC(e.end)) {
                cfw.addExceptionHandler(e.start, e.end, e.handler, exceptionTypeToName(e.type));
            }
        }
        entries.clear();
    }

    private void closeRange(TryInfo info, int exceptionType, int end) {
        if (info.starts[exceptionType] == 0) throw new IllegalStateException("bad exception start");
        entries.add(
                new Entry(
                        info.starts[exceptionType],
                        end,
                        info.handlerLabels[exceptionType],
                        exceptionType,
                        info.depth));
    }

    private static String exceptionTypeToName(int exceptionType) {
        switch (exceptionType) {
            case JAVASCRIPT_EXCEPTION:
                return "org/mozilla/javascript/JavaScriptException";
            case EVALUATOR_EXCEPTION:
                return "org/mozilla/javascript/EvaluatorException";
            case ECMAERROR_EXCEPTION:
                return "org/mozilla/javascript/EcmaError";
            case THROWABLE_EXCEPTION:
                return "java/lang/Throwable";
            case FINALLY_EXCEPTION:
                return null;
            default:
                throw Kit.codeBug();
        }
    }

    private static final class TryInfo {
        final Node finallyBlock;
        final int depth;
        final int[] handlerLabels = new int[EXCEPTION_MAX];
        final int[] starts = new int[EXCEPTION_MAX];
        // The finally block whose inlined copy has suspended this try's handlers
        Node suspendedBy;

        TryInfo(Node finallyBlock, int depth) {
            this.finallyBlock = finallyBlock;
            this.depth = depth;
        }
    }

    private static final class Entry {
        final int start;
        final int end;
        final int handler;
        final int type;
        final int depth;

        Entry(int start, int end, int handler, int type, int depth) {
            this.start = start;
            this.end = end;
            this.handler = handler;
            this.type = type;
            this.depth = depth;
        }
    }
}
