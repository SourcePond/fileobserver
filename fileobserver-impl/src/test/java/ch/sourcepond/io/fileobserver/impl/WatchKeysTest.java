package ch.sourcepond.io.fileobserver.impl;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.ExecutorService;

import static java.lang.Thread.currentThread;
import static java.lang.Thread.interrupted;
import static java.nio.file.StandardWatchEventKinds.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 */
public class WatchKeysTest {
    private final WatchService watchService = mock(WatchService.class);
    private final WatchKey key = mock(WatchKey.class);
    private final Path directory = mock(Path.class);
    private final ExecutorService executor = mock(ExecutorService.class);
    private final ResourceEventProducer producer = mock(ResourceEventProducer.class);
    private final WatchKeys watchKeys = new WatchKeys(watchService, producer);

    @Before
    public void setup() throws Exception {
        when(directory.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)).thenReturn(key);
    }

    @After
    public void tearDown() {
        interrupted();
    }

    @Test
    public void verifyShutdown() throws IOException {
        watchKeys.shutdown();
        verify(watchService).close();
        assertTrue(currentThread().isInterrupted());
    }

    @Test
    public void verifyShutdown_IOExceptionOccurred() throws IOException {
        final IOException expected = new IOException();
        doThrow(expected).when(watchService).close();

        // This should not cause an exception
        watchKeys.shutdown();
        verify(watchService).close();

        assertTrue(currentThread().isInterrupted());
    }

    @Test
    public void verifyIsEmpty() throws IOException {
        assertTrue(watchKeys.isEmpty());
        watchKeys.openWatchKey(directory);
        assertFalse(watchKeys.isEmpty());
        watchKeys.cancelWatchKey(directory);
        assertTrue(watchKeys.isEmpty());
    }

    @Test
    public void verifyCancelWatchKey() throws IOException {
        watchKeys.openWatchKey(directory);
        assertSame(watchKeys, watchKeys.cancelWatchKey(directory));
        verify(key).cancel();
    }

    @Test
    public void verifyCancelUnknownWatchKey() {
        // This should not cause an exception
        watchKeys.cancelWatchKey(directory);
    }

    @Test
    public void verifyWatchServiceExceptionWhenKeyRegistrationFailed() throws IOException {
        final IOException expected = new IOException();
        doThrow(expected).when(directory).register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
        try {
            watchKeys.openWatchKey(directory);
            fail("Exception expected here");
        } catch (final IOException e) {
            assertTrue(e.getCause() instanceof WatchServiceException);
            assertSame(expected, e.getCause().getCause());
        }
    }
}
