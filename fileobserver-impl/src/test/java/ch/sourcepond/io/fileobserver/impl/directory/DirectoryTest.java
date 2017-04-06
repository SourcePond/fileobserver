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
import ch.sourcepond.io.fileobserver.api.FileObserver;
import ch.sourcepond.io.fileobserver.impl.Config;
import ch.sourcepond.io.fileobserver.impl.CopyResourcesTest;
import ch.sourcepond.io.fileobserver.impl.filekey.DefaultFileKeyFactory;
import ch.sourcepond.io.fileobserver.impl.fs.WatchServiceWrapper;
import ch.sourcepond.io.fileobserver.impl.observer.ObserverDispatcher;
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
import static org.junit.Assert.*;
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
    final ExecutorService observerExecutor = newSingleThreadExecutor();
    final DefaultFileKeyFactory keyFactory = new DefaultFileKeyFactory();
    final ObserverDispatcher dispatcher = new ObserverDispatcher();
    final DirectoryFactory factory = new DirectoryFactory(
            keyFactory, dispatcher);
    final Checksum checksum1 = mock(Checksum.class);
    final Checksum checksum2 = mock(Checksum.class);
    final FileObserver observer = mock(FileObserver.class);
    private final Runnable postAddAction = mock(Runnable.class);
    WatchServiceWrapper wrapper;

    @Before
    public void setupFactories() throws IOException {
        when(watchedRootDir.getKey()).thenReturn(ROOT_DIR_KEY);
        when(watchedSubDir1.getKey()).thenReturn(SUB_DIR_KEY1);
        when(watchedSubDir2.getKey()).thenReturn(SUB_DIR_KEY2);
        when(config.timeout()).thenReturn(TIMEOUT);
        dispatcher.addObserver(observer, postAddAction);
        dispatcher.setDispatcherExecutor(dispatcherExecutor);
        dispatcher.setObserverExecutor(observerExecutor);
        wrapper = new WatchServiceWrapper(getDefault());
        factory.setConfig(config);
        factory.setObserverExecutor(observerExecutor);
        factory.setDirectoryWalkerExecutor(directoryWalkerExecutor);
        factory.setResourcesFactory(resourcesFactory);
    }

    @After
    public void shutdownExecutor() {
        wrapper.close();
        dispatcherExecutor.shutdown();
        observerExecutor.shutdown();
    }

    @Test
    public void getDispatcher() {
        assertSame(dispatcher, factory.getDispatcher());
    }

    @Test
    public void verifyDirectoryFactoryDefaultConstructor() {
        // Should not throw an exception
        new DirectoryFactory(keyFactory, dispatcher);
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
