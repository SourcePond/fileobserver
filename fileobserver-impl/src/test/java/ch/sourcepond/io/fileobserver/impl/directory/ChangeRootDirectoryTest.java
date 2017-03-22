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
import org.mockito.InOrder;

import java.io.IOException;
import java.nio.file.Path;

import static ch.sourcepond.io.checksum.api.Algorithm.SHA256;
import static org.mockito.Mockito.*;

/**
 *
 */
public class ChangeRootDirectoryTest extends DirectoryTest {
    private static final Object SUB_DIR_KEY2 = "subDirKey2";
    private final Resource testfile_111_txt = mock(Resource.class);
    private final Resource testfile_121_txt = mock(Resource.class);
    private final Resource testfile_11_xml = mock(Resource.class);
    private Directory new_root;
    private Directory existing_root_11;
    private Directory existing_root_12;

    @Before
    public void setup() throws IOException {
        when(resourcesFactory.create(SHA256, testfile_111_txt_path)).thenReturn(testfile_111_txt);
        when(resourcesFactory.create(SHA256, testfile_121_txt_path)).thenReturn(testfile_121_txt);
        when(resourcesFactory.create(SHA256, testfile_11_xml_path)).thenReturn(testfile_11_xml);

        setupChecksumAnswer(testfile_111_txt, checksum2);
        setupChecksumAnswer(testfile_121_txt, checksum2);
        setupChecksumAnswer(testfile_11_xml, checksum2);

        new_root = factory.newRoot(wrapper.register(root_dir_path));
        new_root.addDirectoryKey(ROOT_DIR_KEY);

        existing_root_11 = factory.newRoot(wrapper.register(subdir_11_path));
        existing_root_11.addDirectoryKey(SUB_DIR_KEY1);
        existing_root_12 = factory.newRoot(wrapper.register(subdir_12_path));
        existing_root_12.addDirectoryKey(SUB_DIR_KEY2);

        existing_root_11 = existing_root_11.rebase(new_root);
        existing_root_12 = existing_root_12.rebase(new_root);
    }

    private FileKey toKey(final Object pDirectoryKey, final Path pBasePath, final Path pPath) {
        return argThat(k -> pDirectoryKey.equals(k.directoryKey()) && pBasePath.relativize(pPath).equals(k.relativePath()));
    }

    @Test
    public void forceInformModifiedAfterRebase() throws IOException {
        existing_root_11.informIfChanged(new_root, observers, testfile_111_txt_path);
        existing_root_12.informIfChanged(new_root, observers, testfile_121_txt_path);
        final InOrder order = inOrder(observer);
        order.verify(observer).supplement(toKey(SUB_DIR_KEY1, subdir_11_path, testfile_111_txt_path), toKey(ROOT_DIR_KEY, root_dir_path, testfile_111_txt_path));
        order.verify(observer).modified(toKey(ROOT_DIR_KEY, root_dir_path, testfile_111_txt_path), eq(testfile_111_txt_path));
        order.verify(observer).supplement(toKey(SUB_DIR_KEY2, subdir_12_path, testfile_121_txt_path), toKey(ROOT_DIR_KEY, root_dir_path, testfile_121_txt_path));
        order.verify(observer).modified(toKey(ROOT_DIR_KEY, root_dir_path, testfile_121_txt_path), eq(testfile_121_txt_path));
        order.verifyNoMoreInteractions();
    }

    @Test
    public void forceInformModifiedAfterSubRootUnregistration() throws IOException {
        existing_root_11.removeDirectoryKey(SUB_DIR_KEY1, observers);
        existing_root_12.removeDirectoryKey(SUB_DIR_KEY2, observers);
        verify(observer).discard(toKey(SUB_DIR_KEY1, subdir_11_path, subdir_11_path));
        verify(observer).discard(toKey(SUB_DIR_KEY2, subdir_12_path, subdir_12_path));
        existing_root_11.informIfChanged(new_root, observers, testfile_111_txt_path);
        existing_root_12.informIfChanged(new_root, observers, testfile_121_txt_path);
        final InOrder order = inOrder(observer);
        order.verify(observer).modified(toKey(ROOT_DIR_KEY, root_dir_path, testfile_111_txt_path), eq(testfile_111_txt_path));
        order.verify(observer).modified(toKey(ROOT_DIR_KEY, root_dir_path, testfile_121_txt_path), eq(testfile_121_txt_path));
        order.verifyNoMoreInteractions();
    }
}
