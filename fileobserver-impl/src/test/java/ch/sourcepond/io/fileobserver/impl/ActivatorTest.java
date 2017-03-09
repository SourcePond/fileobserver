package ch.sourcepond.io.fileobserver.impl;


import ch.sourcepond.io.fileobserver.impl.directory.DirectoryScanner;
import ch.sourcepond.io.fileobserver.impl.fs.VirtualRoot;
import ch.sourcepond.io.fileobserver.spi.WatchedDirectory;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.spi.FileSystemProvider;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by rolandhauser on 08.02.17.
 */
public class ActivatorTest {
    private final DirectoryScanner directoryScanner = mock(DirectoryScanner.class);
    private final FileSystem fs = mock(FileSystem.class);
    private final FileSystemProvider provider = mock(FileSystemProvider.class);
    private final BasicFileAttributes attrs = mock(BasicFileAttributes.class);
    private final Path directory = mock(Path.class);
    private final Path secondDirectory = mock(Path.class);
    private final VirtualRoot virtualRoot = mock(VirtualRoot.class);
    private final WatchedDirectory watchedDirectory = mock(WatchedDirectory.class);
    private final WatchedDirectory secondWatchedDirectory = mock(WatchedDirectory.class);
    private final Activator manager = new Activator(virtualRoot, directoryScanner);

    @Before
    public void setup() throws IOException {
        when(directory.getFileSystem()).thenReturn(fs);
        when(secondDirectory.getFileSystem()).thenReturn(fs);
        when(fs.provider()).thenReturn(provider);
        when(provider.readAttributes(directory, BasicFileAttributes.class)).thenReturn(attrs);
        when(provider.readAttributes(secondDirectory, BasicFileAttributes.class)).thenReturn(attrs);
        when(attrs.isDirectory()).thenReturn(true);
        when(watchedDirectory.getKey()).thenReturn(TestKey.TEST_KEY);
        when(watchedDirectory.getDirectory()).thenReturn(directory);
        when(secondWatchedDirectory.getKey()).thenReturn(TestKey.TEST_KEY1);
        when(secondWatchedDirectory.getDirectory()).thenReturn(secondDirectory);
    }

    @Test
    public void empty() {

    }
}
