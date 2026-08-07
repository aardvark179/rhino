/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript.tools.shell;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.ScriptableObject;

/**
 * Holds a pool of additional, fully initialised {@link Context}s, each running on its own
 * background thread, so that the resource cost of keeping many initialised contexts alive at once
 * can be measured. Any script run by the primary context can also be submitted to every background
 * context via {@link #runInAll(Script)}. Output from {@code print} and {@code console.log} in these
 * background contexts is discarded so that only the primary context's output is visible.
 */
public class BackgroundContexts {
    private final List<Worker> workers = new ArrayList<>();

    public BackgroundContexts(ShellContextFactory factory, int count) {
        for (int i = 0; i < count; i++) {
            Worker worker = new Worker(factory, i);
            workers.add(worker);
            worker.start();
        }
        for (Worker worker : workers) {
            worker.awaitReady();
        }
    }

    /** Submit a script for execution in every background context. */
    public void runInAll(Script script) {
        for (Worker worker : workers) {
            worker.submit(script);
        }
    }

    /** Stop all background threads. Does not wait for in-flight scripts to finish. */
    public void shutdown() {
        for (Worker worker : workers) {
            worker.shutdown();
        }
    }

    private static final class Worker extends Thread {
        private static final Object SHUTDOWN = new Object();

        private final ShellContextFactory factory;
        private final BlockingQueue<Object> queue = new LinkedBlockingQueue<>();
        private final Global global = new Global();
        private final CountDownLatch ready = new CountDownLatch(1);

        Worker(ShellContextFactory factory, int index) {
            super("rhino-background-context-" + index);
            this.factory = factory;
            setDaemon(true);
        }

        void submit(Script script) {
            queue.add(script);
        }

        void shutdown() {
            queue.add(SHUTDOWN);
        }

        void awaitReady() {
            try {
                ready.await();
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }

        @Override
        public void run() {
            try (Context cx = factory.enterContext()) {
                global.init(cx);
                PrintStream discard = new PrintStream(OutputStream.nullOutputStream());
                global.setOut(discard);
                global.setErr(discard);
                ready.countDown();

                Object job;
                while ((job = queue.take()) != SHUTDOWN) {
                    try {
                        ((Script) job)
                                .exec(
                                        cx,
                                        global,
                                        ScriptableObject.getTopLevelScope(global).getGlobalThis());
                    } catch (RhinoException | VirtualMachineError ignored) {
                        // Background contexts only exist to exercise resource usage; a
                        // failure here has no bearing on the primary context's result.
                    }
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
