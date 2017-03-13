package ch.sourcepond.io.fileobserver.impl.fs;

import ch.sourcepond.io.fileobserver.impl.directory.DirectoryFactory;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.WatchService;
import java.nio.file.spi.FileSystemProvider;
import java.util.concurrent.ExecutorService;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by rolandhauser on 10.03.17.
 */
public class DedicatedFileSystemFactoryTest {
    private final FileSystemProvider provider = mock(FileSystemProvider.class);
    private final FileSystem fs = mock(FileSystem.class);
    private final VirtualRoot virtualRoot = mock(VirtualRoot.class);
    private final WatchService watchService = mock(WatchService.class);
    private final ExecutorService directoryWalkerExecutor = mock(ExecutorService.class);
    private final DirectoryFactory directoryFactory = mock(DirectoryFactory.class);
    private final DedicatedFileSystemFactory factory = new DedicatedFileSystemFactory(directoryFactory, directoryWalkerExecutor);

    @Before
    public void setup() throws IOException {
        when(fs.provider()).thenReturn(provider);
        when(fs.newWatchService()).thenReturn(watchService);
    }

    @Test
    public void verifyActivatorConstructor() {
        new DedicatedFileSystemFactory(directoryFactory);
    }

    @Test
    public void getDirectoryFactory() {
        assertSame(directoryFactory, factory.getDirectoryFactory());
    }

    @Test
    public void newFs() throws IOException {
        final DedicatedFileSystem dfs = factory.openFileSystem(virtualRoot, fs);
        dfs.close();
        verify(watchService).close();
    }
}
