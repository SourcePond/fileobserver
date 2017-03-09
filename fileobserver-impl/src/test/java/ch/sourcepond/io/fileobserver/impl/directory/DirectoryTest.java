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

import ch.sourcepond.io.checksum.api.CalculationObserver;
import ch.sourcepond.io.checksum.api.Checksum;
import ch.sourcepond.io.checksum.api.Resource;
import ch.sourcepond.io.checksum.api.ResourcesFactory;
import ch.sourcepond.io.fileobserver.api.FileObserver;
import ch.sourcepond.io.fileobserver.impl.CopyResourcesTest;
import ch.sourcepond.io.fileobserver.impl.filekey.DefaultFileKeyFactory;
import ch.sourcepond.io.fileobserver.impl.fs.WatchServiceWrapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.ExecutorService;

import static ch.sourcepond.io.fileobserver.impl.directory.Directory.TIMEOUT;
import static java.nio.file.FileSystems.getDefault;
import static java.util.Arrays.asList;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

/**
 *
 */
public abstract class DirectoryTest extends CopyResourcesTest {
    static final Object ROOT_DIR_KEY = "rootDirKey";
    static final Object SUB_DIR_KEY1 = "subDirKey1";
    final ResourcesFactory resourcesFactory = mock(ResourcesFactory.class);
    final ExecutorService directoryWalkerExecutor = newSingleThreadExecutor();
    final ExecutorService observerExecutor = newSingleThreadExecutor();
    final DirectoryFactory factory = new DirectoryFactory(
            resourcesFactory,
            new DefaultFileKeyFactory(),
            directoryWalkerExecutor,
            observerExecutor);
    final Checksum checksum1 = mock(Checksum.class);
    final Checksum checksum2 = mock(Checksum.class);
    final FileObserver observer = mock(FileObserver.class);
    final Collection<FileObserver> observers = asList(observer);
    WatchServiceWrapper wrapper;

    @Before
    public void setupFactories() throws IOException {
        wrapper = new WatchServiceWrapper(getDefault().newWatchService());
    }

    @After
    public void shutdownExecutor() {
        observerExecutor.shutdown();
        directoryWalkerExecutor.shutdown();
        wrapper.close();
    }

    @Test
    public void verifyDirectoryFactoryDefaultConstructor() {
        // Should not throw an exception
        new DirectoryFactory();
    }

    void setupChecksumAnswer(final Resource pResource, final Checksum pChecksum2) {
        doAnswer(invocationOnMock -> {
            final CalculationObserver cobsrv = invocationOnMock.getArgument(1);
            cobsrv.done(checksum1, pChecksum2);
            return null;
        }).when(pResource).update(eq(TIMEOUT), notNull());
    }
}
