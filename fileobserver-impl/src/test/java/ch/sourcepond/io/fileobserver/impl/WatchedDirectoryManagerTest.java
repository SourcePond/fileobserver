package ch.sourcepond.io.fileobserver.impl;


import ch.sourcepond.io.fileobserver.impl.directory.Directories;
import ch.sourcepond.io.fileobserver.spi.WatchedDirectory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.spi.FileSystemProvider;

import static ch.sourcepond.io.fileobserver.impl.TestKey.TEST_KEY;
import static org.mockito.Mockito.*;

/**
 * Created by rolandhauser on 08.02.17.
 */
public class WatchedDirectoryManagerTest {

    private final FileSystem fs = mock(FileSystem.class);
    private final FileSystemProvider provider = mock(FileSystemProvider.class);
    private final BasicFileAttributes attrs = mock(BasicFileAttributes.class);
    private final Path directory = mock(Path.class);
    private final Path differentDirectory = mock(Path.class);
    private final Directories directories = mock(Directories.class);
    private final WatchedDirectory watchedDirectory = mock(WatchedDirectory.class);
    private final WatchedDirectoryManager manager = new WatchedDirectoryManager(directories);

    @Before
    public void setup() throws IOException {
        when(directory.getFileSystem()).thenReturn(fs);
        when(differentDirectory.getFileSystem()).thenReturn(fs);
        when(fs.provider()).thenReturn(provider);
        when(provider.readAttributes(directory, BasicFileAttributes.class)).thenReturn(attrs);
        when(provider.readAttributes(differentDirectory, BasicFileAttributes.class)).thenReturn(attrs);
        when(attrs.isDirectory()).thenReturn(true);
        when(watchedDirectory.getKey()).thenReturn((Enum) TestKey.TEST_KEY);
        when(watchedDirectory.getDirectory()).thenReturn(directory);
    }

    @Test(expected = NullPointerException.class)
    public void bindNull() {
        manager.bind(null);
    }

    @Test(expected = NullPointerException.class)
    public void bindKeyIsNull() {
        when(watchedDirectory.getKey()).thenReturn(null);
        manager.bind(watchedDirectory);
    }

    @Test(expected = NullPointerException.class)
    public void bindDirectoryIsNull() {
        when(watchedDirectory.getDirectory()).thenReturn(null);
        manager.bind(watchedDirectory);
    }


    @Test(expected = IllegalArgumentException.class)
    public void bindPathIsNotADirectory() {
        when(attrs.isDirectory()).thenReturn(false);
        manager.bind(watchedDirectory);
    }

    @Test
    public void bindFailed() throws IOException {
        final IOException expected = new IOException();
        doThrow(expected).when(directories).addRoot(TEST_KEY, directory);

        // This should not cause an exception
        manager.bind(watchedDirectory);
    }

    @Test
    public void bindUnbind() throws IOException {
        manager.bind(watchedDirectory);
        manager.unbind(watchedDirectory);
        manager.bind(watchedDirectory);
        manager.unbind(watchedDirectory);

        final InOrder order = inOrder(directories);
        order.verify(directories).addRoot(TEST_KEY, directory);
        order.verify(directories).removeRoot(directory);
        order.verify(directories).addRoot(TEST_KEY, directory);
        order.verify(directories).removeRoot(directory);
    }

    @Test
    public void rebindKeyWithSameDirectory() throws IOException {
        manager.bind(watchedDirectory);
        manager.bind(watchedDirectory);

        // Because the directory was the same, this should have been
        // called only once
        verify(directories).addRoot(TEST_KEY, directory);
    }

    @Test
    public void rebindKeyWithDifferentDirectory() throws IOException {
        manager.bind(watchedDirectory);
        when(watchedDirectory.getDirectory()).thenReturn(differentDirectory);
        manager.bind(watchedDirectory);

        verify(directories).addRoot(TEST_KEY, directory);
        verify(directories).addRoot(TEST_KEY, differentDirectory);
        verify(directories).removeRoot(directory);
    }
}
