package ch.sourcepond.io.fileobserver.impl;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.ClosedFileSystemException;
import java.nio.file.FileSystem;
import java.nio.file.Path;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Created by rolandhauser on 30.01.17.
 */
public class WatchServicesTest {
    private static final String ANY_MESSAGE = "anyMessage";
    private final WatchKeysFactory watchKeysFactory = mock(WatchKeysFactory.class);
    private final WatchKeys watchKeys = mock(WatchKeys.class);
    private final Path directory = mock(Path.class);
    private final FileSystem fs = mock(FileSystem.class);
    private final WatchServices watchServices = new WatchServices(watchKeysFactory);

    @Before
    public void setup() {
        when(directory.getFileSystem()).thenReturn(fs);
        when(watchKeysFactory.createKeys(fs)).thenReturn(watchKeys);
        when(watchKeys.cancelWatchKey(directory)).thenReturn(watchKeys);
    }

    @Test
    public void shutdown() throws IOException {
        watchServices.openWatchKey(directory);
        verify(watchKeys).openWatchKey(directory);
        watchServices.shutdown();
        verify(watchKeys).shutdown();
        watchServices.shutdown();
        verifyNoMoreInteractions(watchKeys);
    }

    @Test
    public void openWatchKey_IOException_Occurred() {
        final WatchServiceException expected = new WatchServiceException(new IOException(ANY_MESSAGE));
        doThrow(expected).when(watchKeysFactory).createKeys(fs);
        try {
            watchServices.openWatchKey(directory);
            fail("Exception expected");
        } catch (final IOException e) {
            assertEquals(ANY_MESSAGE, e.getMessage());
            assertSame(expected, e.getCause());
        }
    }

    @Test
    public void openAndCancelWatchKey() throws IOException {
        watchServices.openWatchKey(directory);
        verify(watchKeys).openWatchKey(directory);
        verify(watchKeysFactory).createKeys(fs);
        watchServices.cancelWatchKey(directory);
        verify(watchKeys).cancelWatchKey(directory);

        // The previously created object should be cached
        watchServices.openWatchKey(directory);
        verifyNoMoreInteractions(watchKeysFactory);
    }

    @Test
    public void openAndCancelWatchKey_KeysAreEmpty() throws IOException {
        watchServices.openWatchKey(directory);
        verify(watchKeys).openWatchKey(directory);

        when(watchKeys.isEmpty()).thenReturn(true);
        watchServices.cancelWatchKey(directory);
        verify(watchKeys).cancelWatchKey(directory);

        // The previously created object should have been removed
        watchServices.openWatchKey(directory);
        verify(watchKeysFactory, times(2)).createKeys(fs);
    }

    @Test
    public void openWatchKey_FsClosed() throws IOException {
        watchServices.openWatchKey(directory);
        final ClosedFileSystemException expected = new ClosedFileSystemException();
        doThrow(expected).when(watchKeys).openWatchKey(directory);
        try {
            watchServices.openWatchKey(directory);
            fail("Exception expected");
        } catch (final ClosedFileSystemException e) {
            assertSame(expected, e);
        }
        verify(watchKeys).shutdown();
    }
}
