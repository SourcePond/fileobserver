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
package ch.sourcepond.io.fileobserver.impl.fs;

import ch.sourcepond.io.checksum.api.ResourcesFactory;
import ch.sourcepond.io.fileobserver.impl.Config;
import ch.sourcepond.io.fileobserver.impl.VirtualRoot;
import ch.sourcepond.io.fileobserver.impl.directory.DirectoryFactory;
import ch.sourcepond.io.fileobserver.impl.listener.ListenerManager;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.WatchService;
import java.nio.file.spi.FileSystemProvider;
import java.util.concurrent.ExecutorService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 *
 */
public class DedicatedFileSystemFactoryTest {
    private final FileSystemProvider provider = mock(FileSystemProvider.class);
    private final FileSystem fs = mock(FileSystem.class);
    private final Config config = mock(Config.class);
    private final ResourcesFactory resourcesFactory = mock(ResourcesFactory.class);
    private final VirtualRoot virtualRoot = mock(VirtualRoot.class);
    private final WatchService watchService = mock(WatchService.class);
    private final ExecutorService dispatchExecutor = mock(ExecutorService.class);
    private final ExecutorService directoryWalkerExecutor = mock(ExecutorService.class);
    private final DirectoryFactory directoryFactory = mock(DirectoryFactory.class);
    private final ListenerManager dispatcher = mock(ListenerManager.class);
    private final DedicatedFileSystemFactory factory = new DedicatedFileSystemFactory(directoryFactory,
            dispatcher, directoryWalkerExecutor);

    @Before
    public void setup() throws IOException {
        factory.setConfig(config);
        when(fs.provider()).thenReturn(provider);
        when(fs.newWatchService()).thenReturn(watchService);
    }

    @Test
    public void shutdown() {
        factory.setExecutors(directoryWalkerExecutor, dispatchExecutor);
        factory.shutdown();
        verify(directoryFactory).shutdown();
        verify(dispatchExecutor).shutdown();
    }

    @Test
    public void setResourceFactory() {
        factory.setResourcesFactory(resourcesFactory);
        verify(directoryFactory).setResourcesFactory(resourcesFactory);
    }

    @Test
    public void setExecutors() {
        factory.setExecutors(directoryWalkerExecutor, dispatchExecutor);
        verify(directoryFactory).setDirectoryWalkerExecutor(directoryWalkerExecutor);
    }

    @Test
    public void setConfig() {
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
