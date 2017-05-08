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
package ch.sourcepond.io.fileobserver.impl.directory;

import ch.sourcepond.io.checksum.api.*;
import ch.sourcepond.io.fileobserver.api.PathChangeListener;
import ch.sourcepond.io.fileobserver.impl.Config;
import ch.sourcepond.io.fileobserver.impl.CopyResourcesTest;
import ch.sourcepond.io.fileobserver.impl.dispatch.DefaultDispatchKeyFactory;
import ch.sourcepond.io.fileobserver.impl.fs.WatchServiceWrapper;
import ch.sourcepond.io.fileobserver.impl.listener.EventDispatcher;
import ch.sourcepond.io.fileobserver.impl.listener.ListenerManager;
import ch.sourcepond.io.fileobserver.impl.pending.PendingEventRegistry;
import ch.sourcepond.io.fileobserver.spi.WatchedDirectory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static java.nio.file.FileSystems.getDefault;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.*;

/**
 *
 */
public abstract class DirectoryTest extends CopyResourcesTest {
    static final long TIMEOUT = 2000;
    static final Object ROOT_DIR_KEY = "rootDirKey";
    static final Object SUB_DIR_KEY1 = "subDirKey1";
    static final Object SUB_DIR_KEY2 = "subDirKey2";
    final WatchedDirectory watchedRootDir = mock(WatchedDirectory.class);
    final WatchedDirectory watchedSubDir1 = mock(WatchedDirectory.class);
    final WatchedDirectory watchedSubDir2 = mock(WatchedDirectory.class);
    final Config config =  mock(Config.class);
    final ResourcesFactory resourcesFactory = mock(ResourcesFactory.class);
    private final ExecutorService dispatcherExecutor = newSingleThreadExecutor();
    final Executor directoryWalkerExecutor = directExecutor();
    final ExecutorService listenerExecutor = newSingleThreadExecutor();
    final DefaultDispatchKeyFactory keyFactory = new DefaultDispatchKeyFactory();
    final PendingEventRegistry pendingEventRegistry = mock(PendingEventRegistry.class);
    final ListenerManager manager = new ListenerManager();
    final EventDispatcher dispatcher = manager.getDefaultDispatcher();
    final DirectoryFactory factory = new DirectoryFactory(pendingEventRegistry,
            keyFactory);
    final Checksum checksum1 = mock(Checksum.class);
    final Checksum checksum2 = mock(Checksum.class);
    final PathChangeListener listener = mock(PathChangeListener.class);
    WatchServiceWrapper wrapper;

    @Before
    public void setupFactories() throws IOException {
        doCallRealMethod().when(listener).restrict(any(), same(root_dir_path.getFileSystem()));
        when(watchedRootDir.getKey()).thenReturn(ROOT_DIR_KEY);
        when(watchedSubDir1.getKey()).thenReturn(SUB_DIR_KEY1);
        when(watchedSubDir2.getKey()).thenReturn(SUB_DIR_KEY2);
        when(config.timeout()).thenReturn(TIMEOUT);
        manager.addListener(listener);
        manager.setDispatcherExecutor(dispatcherExecutor);
        manager.setListenerExecutor(listenerExecutor);
        wrapper = new WatchServiceWrapper(getDefault());
        factory.setConfig(config);
        factory.setListenerExecutor(listenerExecutor);
        factory.setDirectoryWalkerExecutor(directoryWalkerExecutor);
        factory.setResourcesFactory(resourcesFactory);
    }

    @After
    public void shutdownExecutor() {
        wrapper.close();
        dispatcherExecutor.shutdown();
        listenerExecutor.shutdown();
    }

    @Test
    public void verifyDirectoryFactoryDefaultConstructor() {
        // Should not throw an exception
        new DirectoryFactory(pendingEventRegistry, keyFactory);
    }

    void setupChecksumAnswer(final Resource pResource, final Checksum pChecksum2) throws IOException {
        doAnswer(invocationOnMock -> {
            final Update upd = mock(Update.class);
            when(upd.getPrevious()).thenReturn(checksum1);
            when(upd.getCurrent()).thenReturn(pChecksum2);
            when(upd.hasChanged()).thenReturn(!checksum1.equals(pChecksum2));
            final UpdateObserver cobsrv = invocationOnMock.getArgument(1);
            cobsrv.done(upd);
            return null;
        }).when(pResource).update(eq(TIMEOUT), notNull());
    }
}
