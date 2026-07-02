/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.Parser;
import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.AstRoot;
import org.mozilla.javascript.ast.FunctionCall;
import org.mozilla.javascript.ast.Name;
import org.mozilla.javascript.ast.NewExpression;
import org.mozilla.javascript.ast.Spread;

public class SpreadArgumentParserTest {

    private static FunctionCall parseAndExtractFirstCall(String src, int version) {
        AtomicReference<FunctionCall> ref = new AtomicReference<>();
        CompilerEnvirons env = new CompilerEnvirons();
        env.setLanguageVersion(version);
        Parser p = new Parser(env);
        AstRoot root = p.parse(src, "eval", 1);
        root.visit(
                node -> {
                    if (ref.get() != null) return false;
                    if (node instanceof FunctionCall && !(node instanceof NewExpression)) {
                        ref.set((FunctionCall) node);
                        return false;
                    }
                    return true;
                });
        assertNotNull(ref.get(), "Expected to find a FunctionCall node");
        return ref.get();
    }

    private static NewExpression parseAndExtractFirstNew(String src) {
        AtomicReference<NewExpression> ref = new AtomicReference<>();
        CompilerEnvirons env = new CompilerEnvirons();
        env.setLanguageVersion(Context.VERSION_ES6);
        Parser p = new Parser(env);
        AstRoot root = p.parse(src, "eval", 1);
        root.visit(
                node -> {
                    if (ref.get() != null) return false;
                    if (node instanceof NewExpression) {
                        ref.set((NewExpression) node);
                        return false;
                    }
                    return true;
                });
        assertNotNull(ref.get(), "Expected to find a NewExpression node");
        return ref.get();
    }

    @Test
    public void spreadOnlyArgument() {
        FunctionCall call = parseAndExtractFirstCall("f(...args)", Context.VERSION_ES6);
        List<AstNode> args = call.getArguments();
        assertEquals(1, args.size());
        Spread spread = assertInstanceOf(Spread.class, args.get(0));
        Name name = assertInstanceOf(Name.class, spread.getExpression());
        assertEquals("args", name.getIdentifier());
    }

    @Test
    public void spreadAsFinalArgument() {
        FunctionCall call = parseAndExtractFirstCall("f(a, b, ...rest)", Context.VERSION_ES6);
        List<AstNode> args = call.getArguments();
        assertEquals(3, args.size());
        assertInstanceOf(Name.class, args.get(0));
        assertInstanceOf(Name.class, args.get(1));
        Spread spread = assertInstanceOf(Spread.class, args.get(2));
        assertEquals("rest", ((Name) spread.getExpression()).getIdentifier());
    }

    @Test
    public void spreadOfArbitraryExpression() {
        FunctionCall call = parseAndExtractFirstCall("f(...a.b.c)", Context.VERSION_ES6);
        List<AstNode> args = call.getArguments();
        assertEquals(1, args.size());
        Spread spread = assertInstanceOf(Spread.class, args.get(0));
        // The expression is not a Spread itself; it's a property access.
        assertNotNull(spread.getExpression());
    }

    @Test
    public void spreadInNewExpression() {
        NewExpression nx = parseAndExtractFirstNew("new Foo(a, ...rest)");
        List<AstNode> args = nx.getArguments();
        assertEquals(2, args.size());
        assertInstanceOf(Name.class, args.get(0));
        Spread spread = assertInstanceOf(Spread.class, args.get(1));
        assertEquals("rest", ((Name) spread.getExpression()).getIdentifier());
    }

    @Test
    public void nestedSpreadInArgument() {
        FunctionCall call = parseAndExtractFirstCall("f(g(...inner))", Context.VERSION_ES6);
        List<AstNode> outerArgs = call.getArguments();
        assertEquals(1, outerArgs.size());
        FunctionCall inner = assertInstanceOf(FunctionCall.class, outerArgs.get(0));
        List<AstNode> innerArgs = inner.getArguments();
        assertEquals(1, innerArgs.size());
        Spread spread = assertInstanceOf(Spread.class, innerArgs.get(0));
        assertEquals("inner", ((Name) spread.getExpression()).getIdentifier());
    }

    @Test
    public void spreadBeforeEs6IsSyntaxError() {
        CompilerEnvirons env = new CompilerEnvirons();
        env.setLanguageVersion(Context.VERSION_1_8);
        Parser p = new Parser(env);
        assertThrows(EvaluatorException.class, () -> p.parse("f(...a)", "eval", 1));
    }
}
