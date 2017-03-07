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
import ch.sourcepond.io.fileobserver.api.FileKey;
import ch.sourcepond.io.fileobserver.api.FileObserver;
import ch.sourcepond.io.fileobserver.impl.CopyResourcesTest;
import ch.sourcepond.io.fileobserver.impl.ExecutorServices;
import ch.sourcepond.io.fileobserver.impl.filekey.DefaultFileKeyFactory;
import ch.sourcepond.io.fileobserver.impl.fs.WatchServiceWrapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.concurrent.ExecutorService;

import static ch.sourcepond.io.checksum.api.Algorithm.SHA256;
import static ch.sourcepond.io.fileobserver.impl.directory.Directory.TIMEOUT;
import static java.lang.Thread.sleep;
import static java.nio.file.FileSystems.getDefault;
import static java.util.Arrays.asList;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.*;

/**
 *
 */
public class SingleRootDirectoryTest extends CopyResourcesTest {
    private static final Object ROOT_DIR_KEY = new Object();
    private static final Object SUB_DIR_KEY = new Object();
    private final ResourcesFactory resourcesFactory = mock(ResourcesFactory.class);
    private final Resource testfile_txt_resource = mock(Resource.class);
    private final Resource testfile_11_xml_resource = mock(Resource.class);
    private final ExecutorService executorService = newSingleThreadExecutor();
    private final ExecutorServices executorServices = mock(ExecutorServices.class);
    private final DirectoryFactory factory = new DirectoryFactory(resourcesFactory, new DefaultFileKeyFactory(), executorServices);
    private final Checksum checksum1 = mock(Checksum.class);
    private final Checksum checksum2 = mock(Checksum.class);
    private final FileObserver observer = mock(FileObserver.class);
    private final Collection<FileObserver> observers = asList(observer);
    private WatchServiceWrapper wrapper;
    private Directory root_dir;
    private Directory subdir_1;

    @Before
    public void setup() throws IOException {
        when(executorServices.getObserverExecutor()).thenReturn(executorService);
        when(resourcesFactory.create(SHA256, testfile_txt_path)).thenReturn(testfile_txt_resource);
        when(resourcesFactory.create(SHA256, testfile_11_xml_path)).thenReturn(testfile_11_xml_resource);
        wrapper = new WatchServiceWrapper(getDefault().newWatchService());
        root_dir = factory.newRoot(wrapper.register(root_dir_path));
        subdir_1 = factory.newBranch(root_dir, wrapper.register(subdir_1_path));
        root_dir.addDirectoryKey(ROOT_DIR_KEY);
    }

    @After
    public void shutdownExecutor() {
        executorService.shutdown();
    }

    private FileKey toKey(final Path pBasePath, final Path pPath) {
        return argThat(k -> pBasePath.relativize(pPath).equals(k.relativePath()));
    }

    private void setupChecksumAnswer(final Resource pResource, final Checksum pChecksum2) {
        doAnswer(invocationOnMock -> {
            final CalculationObserver cobsrv = invocationOnMock.getArgument(1);
            cobsrv.done(checksum1, pChecksum2);
            return null;
        }).when(pResource).update(eq(TIMEOUT), notNull());
    }

    /**
     *
     */
    @Test
    public void rootDirInformIfChangedChecksumsEqual() throws InterruptedException {
        setupChecksumAnswer(testfile_txt_resource, checksum1);
        root_dir.informIfChanged(observers, testfile_txt_path);
        sleep(200);
        verifyZeroInteractions(observer);
    }

    /**
     *
     */
    @Test
    public void rootDirInformIfChangedChecksumsDifferent() throws IOException {
        setupChecksumAnswer(testfile_txt_resource, checksum2);
        root_dir.informIfChanged(observers, testfile_txt_path);
        verify(observer, timeout(200)).modified(toKey(root_dir_path, testfile_txt_path), eq(testfile_txt_path));
        verifyNoMoreInteractions(observer);
    }

    /**
     *
     */
    @Test
    public void rootDirInformIfChangedChecksumsDifferentButNoKeyRegistered() throws InterruptedException {
        setupChecksumAnswer(testfile_txt_resource, checksum2);
        root_dir.removeDirectoryKey(ROOT_DIR_KEY);
        root_dir.informIfChanged(observers, testfile_txt_path);
        sleep(200);
        verifyZeroInteractions(observer);
    }

    /**
     *
     */
    @Test
    public void subDirInformIfChangedChecksumsEqual() throws InterruptedException {
        setupChecksumAnswer(testfile_11_xml_resource, checksum1);
        subdir_1.informIfChanged(observers, testfile_11_xml_path);
        sleep(200);
        verifyZeroInteractions(observer);
    }

    /**
     *
     */
    @Test
    public void subDirInformIfChangedChecksumsDifferent() throws IOException {
        setupChecksumAnswer(testfile_11_xml_resource, checksum2);
        subdir_1.informIfChanged(observers, testfile_11_xml_path);
        verify(observer, timeout(200)).modified(toKey(root_dir_path, testfile_11_xml_path), eq(testfile_11_xml_path));
        verifyNoMoreInteractions(observer);
    }
}
