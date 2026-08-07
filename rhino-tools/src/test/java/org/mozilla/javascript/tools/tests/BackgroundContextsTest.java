/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tools.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.tools.shell.BackgroundContexts;
import org.mozilla.javascript.tools.shell.ShellContextFactory;

/** Test {@link BackgroundContexts}. */
public class BackgroundContextsTest {

    public static final AtomicInteger COUNTER = new AtomicInteger();

    @Test
    public void runsSubmittedScriptInEveryBackgroundContext() throws InterruptedException {
        COUNTER.set(0);
        int workerCount = 3;
        ShellContextFactory factory = new ShellContextFactory();
        BackgroundContexts backgroundContexts = new BackgroundContexts(factory, workerCount);
        try {
            Script script =
                    compile(
                            factory,
                            "print('bg says hi'); console.log('bg log');"
                                    + "org.mozilla.javascript.tools.tests"
                                    + ".BackgroundContextsTest.COUNTER.incrementAndGet();");

            backgroundContexts.runInAll(script);

            long deadline = System.currentTimeMillis() + 5000;
            while (COUNTER.get() < workerCount && System.currentTimeMillis() < deadline) {
                Thread.sleep(10);
            }
            assertEquals(workerCount, COUNTER.get());
        } finally {
            backgroundContexts.shutdown();
        }
    }

    @Test
    public void createsOneThreadPerWorker() {
        int workerCount = 4;
        ShellContextFactory factory = new ShellContextFactory();
        BackgroundContexts backgroundContexts = new BackgroundContexts(factory, workerCount);
        try {
            long matching =
                    Thread.getAllStackTraces().keySet().stream()
                            .filter(t -> t.getName().startsWith("rhino-background-context-"))
                            .count();
            assertTrue(matching >= workerCount);
        } finally {
            backgroundContexts.shutdown();
        }
    }

    private static Script compile(ShellContextFactory factory, String source) {
        try (Context cx = factory.enterContext()) {
            return cx.compileString(source, "background-test.js", 1, null);
        }
    }
}
