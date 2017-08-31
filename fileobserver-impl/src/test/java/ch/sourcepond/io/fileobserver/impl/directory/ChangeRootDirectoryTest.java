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
import ch.sourcepond.io.fileobserver.api.DispatchKey;
import ch.sourcepond.io.fileobserver.api.PathChangeEvent;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

import java.io.IOException;
import java.nio.file.Path;

import static ch.sourcepond.io.checksum.api.Algorithm.SHA256;
import static java.lang.Thread.sleep;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.notNull;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 *
 */
public class ChangeRootDirectoryTest extends DirectoryTest {
    private final Resource testfile_111_txt = mock(Resource.class);
    private final Resource testfile_121_txt = mock(Resource.class);
    private final Resource testfile_11_xml = mock(Resource.class);
    private Directory new_root;
    private Directory existing_root_11;
    private Directory existing_root_12;

    @Before
    public void setup() throws IOException {
        when(resourcesFactory.create(same(SHA256), eq(testfile_111_txt_path))).thenReturn(testfile_111_txt);
        when(resourcesFactory.create(same(SHA256), eq(testfile_121_txt_path))).thenReturn(testfile_121_txt);
        when(resourcesFactory.create(same(SHA256), eq(testfile_11_xml_path))).thenReturn(testfile_11_xml);

        setupChecksumAnswer(testfile_111_txt, checksum2);
        setupChecksumAnswer(testfile_121_txt, checksum2);
        setupChecksumAnswer(testfile_11_xml, checksum2);

        new_root = factory.newRoot(wrapper.register(root_dir_path));
        new_root.addWatchedDirectory(watchedRootDir);

        existing_root_11 = factory.newRoot(wrapper.register(subdir_11_path));
        existing_root_11.addWatchedDirectory(watchedSubDir1);
        existing_root_12 = factory.newRoot(wrapper.register(subdir_12_path));
        existing_root_12.addWatchedDirectory(watchedSubDir2);

        existing_root_11 = existing_root_11.rebase(new_root);
        existing_root_12 = existing_root_12.rebase(new_root);
    }

    private static boolean isKeyEqual(final DispatchKey pKey, final Object pDirectoryKey, final Path pBasePath, final Path pPath) {
        return pDirectoryKey.equals(pKey.getDirectoryKey()) && pBasePath.relativize(pPath).equals(pKey.getRelativePath());
    }

    private DispatchKey toKey(final Object pDirectoryKey, final Path pBasePath, final Path pPath) {
        return argThat(k -> isKeyEqual(k, pDirectoryKey, pBasePath, pPath));
    }

    private PathChangeEvent toEvent(final Object pDirectoryKey, final Path pBasePath, final Path pPath) {
        return argThat(e -> isKeyEqual(e.getKey(), pDirectoryKey, pBasePath, pPath));
    }

    @Test
    public void forceInformModifiedAfterRebase() throws Exception {
        existing_root_11.informIfChanged(dispatcher, new_root, testfile_111_txt_path, false);
        existing_root_12.informIfChanged(dispatcher, new_root, testfile_121_txt_path, false);
        verify(listener).restrict(notNull(), same(root_dir_path.getFileSystem()));
        verify(listener, timeout(500)).modified(toEvent(ROOT_DIR_KEY, root_dir_path, testfile_111_txt_path));
        verify(listener, timeout(500)).modified(toEvent(ROOT_DIR_KEY, root_dir_path, testfile_121_txt_path));
        verify(listener, timeout(500)).modified(toEvent(SUB_DIR_KEY1, subdir_11_path, testfile_111_txt_path));
        verify(listener, timeout(500)).modified(toEvent(SUB_DIR_KEY2, subdir_12_path, testfile_121_txt_path));
        verify(listener, timeout(500)).supplement(toKey(SUB_DIR_KEY1, subdir_11_path, testfile_111_txt_path), toKey(ROOT_DIR_KEY, root_dir_path, testfile_111_txt_path));
        verify(listener, timeout(500)).supplement(toKey(SUB_DIR_KEY2, subdir_12_path, testfile_121_txt_path), toKey(ROOT_DIR_KEY, root_dir_path, testfile_121_txt_path));
        sleep(500);
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void forceInformModifiedAfterSubRootUnregistration() throws Exception {
        existing_root_11.removeWatchedDirectory(dispatcher, watchedSubDir1);
        existing_root_12.removeWatchedDirectory(dispatcher, watchedSubDir2);
        verify(listener, timeout(500)).discard(toKey(SUB_DIR_KEY1, subdir_11_path, subdir_11_path));
        verify(listener, timeout(500)).discard(toKey(SUB_DIR_KEY2, subdir_12_path, subdir_12_path));
        existing_root_11.informIfChanged(dispatcher, new_root, testfile_111_txt_path, false);
        existing_root_12.informIfChanged(dispatcher, new_root, testfile_121_txt_path, false);
        final InOrder order = inOrder(listener);
        order.verify(listener, timeout(500)).modified(toEvent(ROOT_DIR_KEY, root_dir_path, testfile_111_txt_path));
        order.verify(listener, timeout(500)).modified(toEvent(ROOT_DIR_KEY, root_dir_path, testfile_121_txt_path));
        sleep(500);
        order.verifyNoMoreInteractions();
    }
}
