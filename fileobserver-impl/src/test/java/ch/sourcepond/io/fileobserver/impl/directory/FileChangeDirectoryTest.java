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
import ch.sourcepond.io.fileobserver.api.FileKey;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.WatchKey;

import static ch.sourcepond.io.checksum.api.Algorithm.SHA256;
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
        root_dir.addDirectoryKey(ROOT_DIR_KEY);
    }

    private FileKey toKey(final Path pBasePath, final Path pPath) {
        return argThat(k -> pBasePath.relativize(pPath).equals(k.relativePath()));
    }

    /**
     *
     */
    @Test
    public void rootDirInformIfChangedChecksumsEqual() throws Exception {
        setupChecksumAnswer(testfile_txt_resource, checksum1);
        root_dir.informIfChanged(observers, testfile_txt_path);
        verifyZeroInteractions(observer);
    }

    /**
     *
     */
    @Test
    public void rootDirInformIfChangedChecksumsDifferent() throws IOException {
        setupChecksumAnswer(testfile_txt_resource, checksum2);
        root_dir.informIfChanged(observers, testfile_txt_path);
        verify(observer).modified(toKey(root_dir_path, testfile_txt_path), eq(testfile_txt_path));
        verifyNoMoreInteractions(observer);
    }

    /**
     *
     */
    @Test
    public void rootDirInformIfChangedChecksumsDifferentButNoKeyRegistered() throws Exception {
        setupChecksumAnswer(testfile_txt_resource, checksum2);
        root_dir.removeDirectoryKey(ROOT_DIR_KEY);
        root_dir.informIfChanged(observers, testfile_txt_path);
        verifyZeroInteractions(observer);
    }

    /**
     *
     */
    @Test
    public void subDirInformIfChangedChecksumsEqual() throws Exception {
        setupChecksumAnswer(testfile_11_xml_resource, checksum1);
        subdir_1.informIfChanged(observers, testfile_11_xml_path);
        verifyZeroInteractions(observer);
    }

    /**
     *
     */
    @Test
    public void subDirInformIfChangedChecksumsDifferent() throws IOException {
        setupChecksumAnswer(testfile_11_xml_resource, checksum2);
        subdir_1.informIfChanged(observers, testfile_11_xml_path);
        verify(observer).modified(toKey(root_dir_path, testfile_11_xml_path), eq(testfile_11_xml_path));
        verifyNoMoreInteractions(observer);
    }

    /**
     *
     */
    @Test
    public void formerRootInformIfChangedChecksumsEqual() throws Exception {
        subdir_1.addDirectoryKey(SUB_DIR_KEY1);
        setupChecksumAnswer(testfile_11_xml_resource, checksum1);
        subdir_1.informIfChanged(observers, testfile_11_xml_path);
        verifyZeroInteractions(observer);
    }

    /**
     *
     */
    @Test
    public void formerRootInformIfChangedChecksumsDifferent() throws IOException {
        subdir_1.addDirectoryKey(SUB_DIR_KEY1);
        setupChecksumAnswer(testfile_11_xml_resource, checksum2);
        subdir_1.informIfChanged(observers, testfile_11_xml_path);
        verify(observer).modified(toKey(root_dir_path, testfile_11_xml_path), eq(testfile_11_xml_path));
        verify(observer).modified(toKey(subdir_1_path, testfile_11_xml_path), eq(testfile_11_xml_path));
        verifyNoMoreInteractions(observer);
    }

    /**
     *
     */
    @Test
    public void rootDirInformIfFileDiscarded() throws IOException, InterruptedException {
        root_dir.informDiscard(observers, testfile_txt_path);
        verify(observer).discard(toKey(root_dir_path, testfile_txt_path));
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
        root_dir.removeDirectoryKey(ROOT_DIR_KEY, observers);
        verify(observer).discard(toKey(root_dir_path, root_dir_path));

        // This should have no effect
        root_dir.removeDirectoryKey(ROOT_DIR_KEY, observers);
        verifyNoMoreInteractions(observer);
    }

    @Test
    public void informObserverWhenForceInform() throws IOException, InterruptedException {
        root_dir.forceInform(observer);
        verify(observer).modified(toKey(root_dir_path, testfile_txt_path), eq(testfile_txt_path));
        verifyNoMoreInteractions(observer);
    }

    @Test
    public void verifyExceptionInForceInformDoesNotKillThread() throws IOException, InterruptedException {
        // Provocate IOExcepion
        deleteResources();

        // Should not cause an exception
        root_dir.forceInform(observer);
        verifyZeroInteractions(observer);
    }


    @Test
    public void verifyExceptionInObserverDoesNotKillThread() throws IOException, InterruptedException {
        doThrow(IOException.class).when(observer).modified(any(), any());
        root_dir.forceInform(observer);

        // Should not cause an exception
        verify(observer).modified(toKey(root_dir_path, testfile_txt_path), eq(testfile_txt_path));
    }

    @Test
    public void verifyCancelKey() {
        final WatchKey key = root_dir.getWatchKey();
        assertTrue(key.isValid());
        root_dir.cancelKey();
        assertFalse(key.isValid());
    }
}
