package ch.sourcepond.io.fileobserver.impl.fs;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.Path;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

import static java.nio.file.StandardWatchEventKinds.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Created by rolandhauser on 06.03.17.
 */
public class WatchServiceWrapperTest {
    private final Path directory = mock(Path.class);
    private final WatchService watchService = mock(WatchService.class);
    private final WatchKey watchKey = mock(WatchKey.class);
    private final WatchServiceWrapper registrar = new WatchServiceWrapper(watchService);

    @Test
    public void close() throws IOException {
        registrar.close();
        verify(watchService).close();
    }

    @Test
    public void closeIoExceptionOccurred() throws IOException {
        doThrow(IOException.class).when(watchService).close();
        // This should not cause an exception
        registrar.close();
    }

    @Test
    public void poll() {
        registrar.poll();
        verify(watchService).poll();
    }

    @Test
    public void register() throws IOException {
        when(directory.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)).thenReturn(watchKey);
        assertSame(watchKey, registrar.register(directory));
    }

    @Test
    public void registrationFaileBecauseWatchServiceIsClosed() throws IOException {
        final ClosedWatchServiceException expected = new ClosedWatchServiceException();
        doThrow(expected).when(directory).register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
        try {
            registrar.register(directory);
            fail("Exception expected here");
        } catch (final IOException e) {
            assertSame(expected, e.getCause());
        }
    }
}
