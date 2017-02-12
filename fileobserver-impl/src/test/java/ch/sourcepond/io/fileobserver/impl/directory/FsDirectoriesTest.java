package ch.sourcepond.io.fileobserver.impl.directory;

import ch.sourcepond.io.fileobserver.impl.observer.ObserverHandler;
import ch.sourcepond.io.fileobserver.impl.registrar.Registrar;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.WatchKey;
import java.util.HashMap;
import java.util.Map;

import static ch.sourcepond.io.fileobserver.impl.TestKey.TEST_KEY;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Created by rolandhauser on 06.02.17.
 */
public class FsDirectoriesTest {
    private final FileSystem fs = mock(FileSystem.class);
    private final Registrar registrar = mock(Registrar.class);
    private final FsDirectory fsDirectory = mock(FsDirectory.class);
    private final FsDirectory fsSubDirectory = mock(FsDirectory.class);
    private final ObserverHandler handler = mock(ObserverHandler.class);
    private final Path directory = mock(Path.class);
    private final Path subDirectory = mock(Path.class);
    private final WatchKey key = mock(WatchKey.class);
    private final Map<Path, FsDirectory> entries = new HashMap<>();
    private final FsDirectories fsDirectories = new FsDirectoriesFactory().newDirectories(registrar);

    @Before
    public void setup() throws IOException {
        when(directory.getFileSystem()).thenReturn(fs);
        when(fsDirectory.getPath()).thenReturn(directory);
        entries.put(subDirectory, fsSubDirectory);
    }

    @Test
    public void rootAdded() {
        fsDirectories.rootAdded(TEST_KEY, directory, handler);
        verify(registrar).rootAdded(TEST_KEY, directory, handler);
    }

    @Test
    public void initialyInformHandler() throws IOException {
        fsDirectories.initiallyInformHandler(handler);
        verify(registrar).initiallyInformHandler(handler);
    }

    @Test
    public void directoryCreated() throws IOException {
        fsDirectories.directoryCreated(directory, handler);
        verify(registrar).directoryCreated(directory, handler);
    }

    @Test
    public void directoryDeleted() {
        fsDirectories.directoryDeleted(directory);
        verify(registrar).directoryDeleted(directory);
    }

    @Test
    public void directoryDeletedDirUnknown() {
        fsDirectories.directoryDeleted(directory);
        verifyZeroInteractions(directory, subDirectory);
    }

    @Test
    public void getDirectory() {
        when(subDirectory.getParent()).thenReturn(directory);
        when(registrar.getDirectory(directory)).thenReturn(fsDirectory);
        assertSame(fsDirectory, fsDirectories.getDirectory(subDirectory));
    }

    @Test(expected = NullPointerException.class)
    public void getDirectoryParentUnknown() {
        when(subDirectory.getParent()).thenReturn(directory);
        fsDirectories.getDirectory(subDirectory);
    }

    @Test
    public void close() throws IOException {
        fsDirectories.close();
        verify(registrar).close();
    }


    @Test
    public void closeIOException() throws IOException {
        final IOException expected = new IOException();
        doThrow(expected).when(registrar).close();

        // This should not cause an exception
        fsDirectories.close();
    }

    @Test
    public void poll() {
        when(registrar.poll()).thenReturn(key);
        assertSame(key, fsDirectories.poll());
    }
}
