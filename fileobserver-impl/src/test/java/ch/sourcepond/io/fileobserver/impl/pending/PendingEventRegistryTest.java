/*Copyright (C) 2017 Roland Hauser, <sourcepond@gmail.com>

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.*/
package ch.sourcepond.io.fileobserver.impl.pending;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.FileSystem;
import java.util.concurrent.ScheduledExecutorService;

import static java.lang.Thread.currentThread;
import static java.lang.Thread.interrupted;
import static java.lang.Thread.sleep;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.util.concurrent.Executors.newScheduledThreadPool;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

/**
 *
 */
public class PendingEventRegistryTest {
    private final FileSystem fs = mock(FileSystem.class);
    private final PendingEventRegistry registry = new PendingEventRegistry();
    private final ScheduledExecutorService executor = newScheduledThreadPool(1);

    @Before
    public void setup() {
        registry.setModificationLockingTime(200L);
    }

    @After
    public void tearDown() {
        executor.shutdown();
    }

    @Test
    public void awaitIfPendingDropModification() {
        // Should not block
        assertTrue(registry.awaitIfPending(fs, ENTRY_CREATE));
        assertFalse(registry.awaitIfPending(fs, ENTRY_MODIFY));
    }

    @Test(timeout = 500)
    public void awaitIfPendingNoCreatePending() {
        // Should not block
        assertTrue(registry.awaitIfPending(fs, ENTRY_MODIFY));
    }

    @Test(timeout = 500)
    public void awaitIfPendingCreate() {
        // Should not block
        assertTrue(registry.awaitIfPending(fs, ENTRY_CREATE));
    }

    @Test(timeout = 5000)
    public void awaitIfPendingCreationPending() throws Exception {
        // Should not block
        assertTrue(registry.awaitIfPending(fs, ENTRY_CREATE));
        executor.schedule(() -> registry.done(fs), 1000, MILLISECONDS);

        sleep(500);

        // This should block now
        assertTrue(registry.awaitIfPending(fs, ENTRY_MODIFY));
    }

    @Test(timeout = 5000)
    public void awaitIfPendingCreationPendingInterrupted() throws Exception {
        // Should not block
        assertTrue(registry.awaitIfPending(fs, ENTRY_CREATE));
        final Thread th = currentThread();
        executor.schedule(() -> th.interrupt(), 1000, MILLISECONDS);


        sleep(500);

        // This should block now
        assertTrue(registry.awaitIfPending(fs, ENTRY_MODIFY));
        assertTrue(interrupted());
    }
}
