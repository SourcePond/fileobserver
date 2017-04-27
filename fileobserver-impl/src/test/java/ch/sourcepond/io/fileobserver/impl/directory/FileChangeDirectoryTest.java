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

import ch.sourcepond.io.checksum.api.Resource;
import ch.sourcepond.io.fileobserver.api.ChangeEvent;
import ch.sourcepond.io.fileobserver.api.DispatchKey;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.WatchKey;

import static ch.sourcepond.io.checksum.api.Algorithm.SHA256;
import static java.lang.Thread.sleep;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

/**
 *
 */
public class FileChangeDirectoryTest extends DirectoryTest {
    private final Resource testfile_txt_resource = mock(Resource.class);
    private final Resource testfile_11_xml_resource = mock(Resource.class);
    private Directory root_dir;
    private Directory subdir_1;

    @Before
    public void setup() throws IOException {
        when(resourcesFactory.create(SHA256, testfile_txt_path)).thenReturn(testfile_txt_resource);
        when(resourcesFactory.create(SHA256, testfile_11_xml_path)).thenReturn(testfile_11_xml_resource);
        root_dir = factory.newRoot(wrapper.register(root_dir_path));
        subdir_1 = factory.newBranch(root_dir, wrapper.register(subdir_1_path));
        root_dir.addWatchedDirectory(watchedRootDir);
    }

    private static boolean isKeyEqual(final DispatchKey pKey, final Path pBasePath, final Path pPath) {
        return pBasePath.relativize(pPath).equals(pKey.getRelativePath());
    }

    private DispatchKey toKey(final Path pBasePath, final Path pPath) {
        return argThat(k -> isKeyEqual(k, pBasePath, pPath));
    }

    private ChangeEvent toEvent(final Path pBasePath, final Path pPath) {
        return argThat(e -> isKeyEqual(e.getKey(), pBasePath, pPath));
    }

    /**
     *
     */
    @Test
    public void rootDirInformIfChangedChecksumsEqual() throws Exception {
        setupChecksumAnswer(testfile_txt_resource, checksum1);
        root_dir.informIfChanged(dispatcher, testfile_txt_path);
        verifyZeroInteractions(listener);
    }

    /**
     *
     */
    @Test
    public void rootDirInformIfChangedChecksumsDifferent() throws Exception {
        setupChecksumAnswer(testfile_txt_resource, checksum2);
        root_dir.informIfChanged(dispatcher, testfile_txt_path);
        verify(listener, timeout(500)).modified(toEvent(root_dir_path, testfile_txt_path));
        sleep(500);
        verify(listener).restrict(notNull());
        verifyNoMoreInteractions(listener);
    }

    /**
     *
     */
    @Test
    public void rootDirInformIfChangedChecksumsDifferentButNoKeyRegistered() throws Exception {
        setupChecksumAnswer(testfile_txt_resource, checksum2);
        root_dir.remove(watchedRootDir);
        root_dir.informIfChanged(dispatcher, testfile_txt_path);
        verifyZeroInteractions(listener);
    }

    /**
     *
     */
    @Test
    public void subDirInformIfChangedChecksumsEqual() throws Exception {
        setupChecksumAnswer(testfile_11_xml_resource, checksum1);
        subdir_1.informIfChanged(dispatcher, testfile_11_xml_path);
        verifyZeroInteractions(listener);
    }

    /**
     *
     */
    @Test
    public void subDirInformIfChangedChecksumsDifferent() throws Exception {
        setupChecksumAnswer(testfile_11_xml_resource, checksum2);
        subdir_1.informIfChanged(dispatcher, testfile_11_xml_path);
        verify(listener, timeout(500)).modified(toEvent(root_dir_path, testfile_11_xml_path));
        sleep(500);
        verify(listener).restrict(notNull());
        verifyNoMoreInteractions(listener);
    }

    /**
     *
     */
    @Test
    public void formerRootInformIfChangedChecksumsEqual() throws Exception {
        subdir_1.addWatchedDirectory(watchedSubDir1);
        setupChecksumAnswer(testfile_11_xml_resource, checksum1);
        subdir_1.informIfChanged(dispatcher, testfile_11_xml_path);
        verifyZeroInteractions(listener);
    }

    /**
     *
     */
    @Test
    public void formerRootInformIfChangedChecksumsDifferent() throws Exception {
        subdir_1.addWatchedDirectory(watchedSubDir1);
        setupChecksumAnswer(testfile_11_xml_resource, checksum2);
        subdir_1.informIfChanged(dispatcher, testfile_11_xml_path);
        verify(listener, timeout(500)).modified(toEvent(root_dir_path, testfile_11_xml_path));
        verify(listener, timeout(500)).modified(toEvent(subdir_1_path, testfile_11_xml_path));
        sleep(500);
        verify(listener).restrict(notNull());
        verifyNoMoreInteractions(listener);
    }

    /**
     *
     */
    @Test
    public void rootDirInformIfFileDiscarded() throws IOException, InterruptedException {
        root_dir.informDiscard(dispatcher, testfile_txt_path);
        verify(listener, timeout(500)).discard(toKey(root_dir_path, testfile_txt_path));
    }

    @Test
    public void verifyIsDirectParentOf() throws IOException {
        final Directory subdir_11 = factory.newBranch(subdir_1, wrapper.register(subdir_11_path));
        assertTrue(root_dir.isDirectParentOf(subdir_1));
        assertTrue(subdir_1.isDirectParentOf(subdir_11));
        assertFalse(root_dir.isDirectParentOf(subdir_11));
        assertFalse(subdir_1.isDirectParentOf(root_dir));
        assertFalse(subdir_11.isDirectParentOf(root_dir));
    }

    /**
     *
     */
    @Test
    public void checkDiscardAfterDirectoryKeyRemoval() throws IOException, InterruptedException {
        root_dir.removeWatchedDirectory(dispatcher, watchedRootDir);
        verify(listener, timeout(1000)).discard(toKey(root_dir_path, root_dir_path));

        // This should have no effect
        root_dir.removeWatchedDirectory(dispatcher, watchedRootDir);
        verify(listener).restrict(notNull());
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void informObserverWhenForceInform() throws IOException, InterruptedException {
        root_dir.forceInform(dispatcher);
        verify(listener, timeout(500)).modified(toEvent(root_dir_path, testfile_txt_path));
        sleep(500);
        verify(listener).restrict(notNull());
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void verifyExceptionInForceInformDoesNotKillThread() throws IOException, InterruptedException {
        // Provocate IOExcepion
        deleteResources();

        // Should not cause an exception
        root_dir.forceInform(dispatcher);
        verifyZeroInteractions(listener);
    }


    @Test
    public void verifyExceptionInObserverDoesNotKillThread() throws IOException, InterruptedException {
        doThrow(IOException.class).when(listener).modified(any());
        root_dir.forceInform(dispatcher);

        // Should not cause an exception
        verify(listener, timeout(500)).modified(toEvent(root_dir_path, testfile_txt_path));
    }

    @Test
    public void verifyCancelKey() {
        final WatchKey key = root_dir.getWatchKey();
        assertTrue(key.isValid());
        root_dir.cancelKey();
        assertFalse(key.isValid());
    }
}
