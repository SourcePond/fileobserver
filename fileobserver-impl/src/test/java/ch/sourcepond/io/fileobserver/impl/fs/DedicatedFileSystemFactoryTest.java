package ch.sourcepond.io.fileobserver.impl.fs;

import ch.sourcepond.io.checksum.api.ResourcesFactory;
import ch.sourcepond.io.fileobserver.impl.Config;
import ch.sourcepond.io.fileobserver.impl.VirtualRoot;
import ch.sourcepond.io.fileobserver.impl.directory.DirectoryFactory;
import ch.sourcepond.io.fileobserver.impl.observer.ObserverDispatcher;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.WatchService;
import java.nio.file.spi.FileSystemProvider;
import java.util.concurrent.ExecutorService;

import static org.mockito.Mockito.*;

/**
 * Created by rolandhauser on 10.03.17.
 */
public class DedicatedFileSystemFactoryTest {
    private final FileSystemProvider provider = mock(FileSystemProvider.class);
    private final FileSystem fs = mock(FileSystem.class);
    private final Config config = mock(Config.class);
    private final ResourcesFactory resourcesFactory = mock(ResourcesFactory.class);
    private final VirtualRoot virtualRoot = mock(VirtualRoot.class);
    private final WatchService watchService = mock(WatchService.class);
    private final ExecutorService observerExecutor = mock(ExecutorService.class);
    private final ExecutorService directoryWalkerExecutor = mock(ExecutorService.class);
    private final DirectoryFactory directoryFactory = mock(DirectoryFactory.class);
    private final ObserverDispatcher dispatcher = mock(ObserverDispatcher.class);
    private final DedicatedFileSystemFactory factory = new DedicatedFileSystemFactory(directoryFactory, dispatcher, directoryWalkerExecutor);

    @Before
    public void setup() throws IOException {
        when(fs.provider()).thenReturn(provider);
        when(fs.newWatchService()).thenReturn(watchService);
    }

    @Test
    public void setResourceFactory() {
        factory.setResourcesFactory(resourcesFactory);
        verify(directoryFactory).setResourcesFactory(resourcesFactory);
    }

    @Test
    public void setDirectoryWalkerExecutor() {
        factory.setDirectoryWalkerExecutor(directoryWalkerExecutor);
        verify(directoryFactory).setDirectoryWalkerExecutor(directoryWalkerExecutor);
    }

    @Test
    public void setObserverExecutor() {
        factory.setObserverExecutor(observerExecutor);
        verify(directoryFactory).setObserverExecutor(observerExecutor);
    }

    @Test
    public void setConfig() {
        factory.setConfig(config);
        verify(directoryFactory).setConfig(config);
    }

    @Test
    public void verifyActivatorConstructor() {
        new DedicatedFileSystemFactory(directoryFactory, dispatcher);
    }

    @Test
    public void newFs() throws IOException {
        final DedicatedFileSystem dfs = factory.openFileSystem(virtualRoot, fs);
        dfs.close();
        verify(watchService).close();
    }
}
