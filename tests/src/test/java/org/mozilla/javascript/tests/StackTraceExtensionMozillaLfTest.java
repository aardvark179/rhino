package org.mozilla.javascript.tests;

import static org.junit.Assert.assertFalse;

import java.io.FileReader;
import java.io.IOException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.StackStyle;
import org.mozilla.javascript.tools.shell.Global;

public class StackTraceExtensionMozillaLfTest {
    @BeforeClass
    public static void init() {
        RhinoException.setStackStyle(StackStyle.MOZILLA);
    }

    @AfterClass
    public static void terminate() {
        RhinoException.setStackStyle(StackStyle.RHINO);
    }

    private void testTraces(boolean interpretedMode) {
        final ContextFactory factory =
                new ContextFactory() {
                    @Override
                    protected boolean hasFeature(Context cx, int featureIndex) {
                        switch (featureIndex) {
                            case Context.FEATURE_LOCATION_INFORMATION_IN_ERROR:
                                return true;
                            default:
                                return super.hasFeature(cx, featureIndex);
                        }
                    }
                };

        try (Context cx = factory.enterContext()) {
            cx.setLanguageVersion(Context.VERSION_1_8);
            cx.setInterpretedMode(interpretedMode);
            cx.setGeneratingDebug(true);

            Global global = new Global(cx);
            Scriptable root = cx.newObject(global);

            try (FileReader rdr =
                    new FileReader("testsrc/jstests/extensions/stack-traces-mozilla-lf.js")) {
                cx.evaluateReader(root, rdr, "stack-traces-mozilla-lf.js", 1, null);
            }
        } catch (IOException ioe) {
            assertFalse("I/O Error: " + ioe, true);
        }
    }

    @Test
    public void stackTraceInterpreted() {
        testTraces(true);
    }

    @Test
    public void stackTraceCompiled() {
        testTraces(false);
    }
}
