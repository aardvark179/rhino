/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tools.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.tools.shell.Global;
import org.mozilla.javascript.tools.shell.ShellContextFactory;

/**
 * Verify that {@code print} and {@code console.log} output goes to the console of the {@link
 * Global} that owns the calling scope, not to some other, unrelated {@link Global}'s console.
 */
public class GlobalConsoleRoutingTest {

    @Test
    public void consoleLogAndPrintStayWithinTheirOwningGlobal() {
        ShellContextFactory factory = new ShellContextFactory();
        Global ownGlobal = new Global();
        Global unrelatedGlobal = new Global();
        try (Context cx = factory.enterContext()) {
            ownGlobal.init(cx);
            unrelatedGlobal.init(cx);

            ByteArrayOutputStream ownOut = new ByteArrayOutputStream();
            ByteArrayOutputStream unrelatedOut = new ByteArrayOutputStream();
            ownGlobal.setOut(new PrintStream(ownOut));
            unrelatedGlobal.setOut(new PrintStream(unrelatedOut));

            cx.evaluateString(
                    ownGlobal, "print('own print'); console.log('own log');", "test.js", 1, null);

            assertTrue(ownOut.toString(StandardCharsets.UTF_8).contains("own print"));
            assertTrue(ownOut.toString(StandardCharsets.UTF_8).contains("own log"));
            assertEquals("", unrelatedOut.toString(StandardCharsets.UTF_8));
        }
    }
}
