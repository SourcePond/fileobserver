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

import ch.sourcepond.io.fileobserver.impl.Config;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.spi.FileSystemProvider;
import java.util.concurrent.ScheduledExecutorService;

import static java.lang.Thread.*;
import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.util.concurrent.Executors.newScheduledThreadPool;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 */
public class PendingEventRegistryTest {
    private final Config config = mock(Config.class);
    private final FileSystem fs = mock(FileSystem.class);
    private final FileSystemProvider provider = mock(FileSystemProvider.class);
    private final Path path = mock(Path.class);
    private final Path child = mock(Path.class);
    private final PendingEventRegistry registry = new PendingEventRegistry();
    private final ScheduledExecutorService executor = newScheduledThreadPool(1);

    @Before
    public void setup() throws IOException {
        when(path.getFileSystem()).thenReturn(fs);
        when(child.getFileSystem()).thenReturn(fs);
        when(child.getParent()).thenReturn(path);
        when(fs.provider()).thenReturn(provider);
        when(config.modificationLockingMillis()).thenReturn(200L);
        when(config.pendingTimeoutMillis()).thenReturn(3000L);
        registry.setConfig(config);
    }

    @After
    public void tearDown() {
        executor.shutdown();
    }

    @Test
    public void awaitIfPendingDropModification() {
        // Should not block
        assertTrue(registry.awaitIfPending(path, ENTRY_CREATE));
        assertFalse(registry.awaitIfPending(path, ENTRY_MODIFY));
    }

    @Test(timeout = 500)
    public void awaitIfPendingNoCreatePending() {
        // Should not block
        assertTrue(registry.awaitIfPending(path, ENTRY_MODIFY));
    }

    @Test(timeout = 500)
    public void awaitIfPendingCreate() {
        // Should not block
        assertTrue(registry.awaitIfPending(path, ENTRY_CREATE));
    }

    @Test(timeout = 5000)
    public void awaitIfPendingCreationPending() throws Exception {
        // Should not block
        assertTrue(registry.awaitIfPending(path, ENTRY_CREATE));
        executor.schedule(() -> registry.done(path), 1000, MILLISECONDS);

        sleep(500);

        // This should block now
        assertTrue(registry.awaitIfPending(path, ENTRY_MODIFY));
    }

    @Test(timeout = 5000)
    public void awaitIfPendingCreationPendingInterrupted() throws Exception {
        // Should not block
        assertTrue(registry.awaitIfPending(path, ENTRY_CREATE));
        final Thread th = currentThread();
        executor.schedule(() -> th.interrupt(), 1000, MILLISECONDS);


        sleep(500);

        // This should block now
        assertTrue(registry.awaitIfPending(path, ENTRY_MODIFY));
        assertTrue(interrupted());
    }

    @Test(timeout = 5000)
    public void timeoutIfNotNotified() throws Exception {
        // Should not block
        assertTrue(registry.awaitIfPending(path, ENTRY_CREATE));
        sleep(500);
        assertTrue(registry.awaitIfPending(path, ENTRY_MODIFY));
    }

    @Ignore
    @Test
    public void removeChildOfDeleteDirectory() throws Exception {
        doThrow(NoSuchFileException.class).when(provider).readAttributes(path, BasicFileAttributes.class, NOFOLLOW_LINKS);
        assertTrue(registry.awaitIfPending(path, ENTRY_CREATE));
        assertTrue(registry.awaitIfPending(child, ENTRY_CREATE));
        sleep(500);
        executor.schedule(() -> registry.done(path), 200L, MILLISECONDS);

        // This should not block
        assertTrue(registry.awaitIfPending(child, ENTRY_CREATE));
    }
}
