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
package ch.sourcepond.io.fileobserver.impl.diff;

import ch.sourcepond.io.checksum.api.Resource;
import ch.sourcepond.io.checksum.api.Update;
import ch.sourcepond.io.checksum.api.UpdateObserver;
import ch.sourcepond.io.fileobserver.api.FileKey;
import ch.sourcepond.io.fileobserver.api.FileObserver;
import ch.sourcepond.io.fileobserver.impl.CopyResourcesTest;
import ch.sourcepond.io.fileobserver.impl.directory.Directory;
import ch.sourcepond.io.fileobserver.impl.filekey.DefaultFileKeyFactory;
import ch.sourcepond.io.fileobserver.impl.fs.DedicatedFileSystem;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.concurrent.Executor;

import static ch.sourcepond.io.fileobserver.impl.directory.Directory.TIMEOUT;
import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.Files.delete;
import static java.nio.file.Files.walkFileTree;
import static java.util.Arrays.asList;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.*;

/**
 *
 */
public class DiffObserverTest extends CopyResourcesTest {
    private static final Object DIRECTORY_KEY = "directoryKey";
    private final DefaultFileKeyFactory keyFactory = new DefaultFileKeyFactory();
    private final DedicatedFileSystem fs = mock(DedicatedFileSystem.class);
    private final Executor observerExecutor = directExecutor();
    private final FileObserver observer = mock(FileObserver.class);
    private final Collection<FileObserver> observers = asList(observer);
    private final Directory root_dir = mock(Directory.class);
    private final Directory subdir_1 = mock(Directory.class);
    private final Directory subdir_11 = mock(Directory.class);
    private final Directory subdir_111 = mock(Directory.class);
    private final Directory subdir_12 = mock(Directory.class);
    private final Directory subdir_2 = mock(Directory.class);
    private final Directory subdir_21 = mock(Directory.class);
    private final Directory subdir_211 = mock(Directory.class);
    private final Directory subdir_22 = mock(Directory.class);
    private final Resource resource = mock(Resource.class);
    private final Update update = mock(Update.class);
    private final DiffObserverFactory factory = new DiffObserverFactory(observerExecutor);
    private DiffObserver diff;

    private void informModified(final Path pPath) throws Exception {
        walkFileTree(pPath, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                diff.modified(keyFactory.newKey(DIRECTORY_KEY, pPath.relativize(file)), file);
                return CONTINUE;
            }
        });
    }

    private void informDiscard(final Path pPath) throws Exception {
        walkFileTree(pPath, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                diff.discard(keyFactory.newKey(DIRECTORY_KEY, pPath.relativize(file)));
                return CONTINUE;
            }
        });
    }

    private FileKey key(final Path pRoot, final Path pFile) {
        return keyFactory.newKey(DIRECTORY_KEY, pRoot.relativize(pFile));
    }

    private void setupUpdate(final Resource pResource, final Update pUpdate, boolean pHasChanged) throws Exception {
        when(pUpdate.hasChanged()).thenReturn(pHasChanged);
        doAnswer(inv -> {
            final UpdateObserver obsrv = (UpdateObserver) inv.getArgument(1);
            obsrv.done(pUpdate);
            return null;
        }).when(pResource).update(eq(TIMEOUT), notNull());
    }

    @Before
    public void setup() throws Exception {
        setupUpdate(resource, update, true);

        when(fs.getDirectory(root_dir_path)).thenReturn(root_dir);
        when(fs.getDirectory(subdir_1_path)).thenReturn(subdir_1);
        when(fs.getDirectory(subdir_11_path)).thenReturn(subdir_11);
        when(fs.getDirectory(subdir_111_path)).thenReturn(subdir_111);
        when(fs.getDirectory(subdir_12_path)).thenReturn(subdir_12);
        when(fs.getDirectory(subdir_2_path)).thenReturn(subdir_2);
        when(fs.getDirectory(subdir_21_path)).thenReturn(subdir_21);
        when(fs.getDirectory(subdir_211_path)).thenReturn(subdir_211);
        when(fs.getDirectory(subdir_22_path)).thenReturn(subdir_22);

        when(root_dir.getResource(notNull())).thenReturn(resource);
        when(subdir_1.getResource(notNull())).thenReturn(resource);
        when(subdir_11.getResource(notNull())).thenReturn(resource);
        when(subdir_111.getResource(notNull())).thenReturn(resource);
        when(subdir_12.getResource(notNull())).thenReturn(resource);
        when(subdir_2.getResource(notNull())).thenReturn(resource);
        when(subdir_21.getResource(notNull())).thenReturn(resource);
        when(subdir_211.getResource(notNull())).thenReturn(resource);
        when(subdir_22.getResource(notNull())).thenReturn(resource);

        diff = factory.createObserver(fs, observers);
    }

    @Test
    public void verfiyDefaultFactoryConstructor() {
        new DiffObserverFactory();
    }

    @Test
    public void simpleRelocation() throws Exception {
        when(update.hasChanged()).thenReturn(true);
        informDiscard(root_dir_path);
        informModified(root_dir_path);

        diff.finalizeRelocation();
        verify(observer).modified(key(root_dir_path, testfile_1111_txt_path), testfile_1111_txt_path);
        verify(observer).modified(key(root_dir_path, testfile_111_txt_path), testfile_111_txt_path);
        verify(observer).modified(key(root_dir_path, testfile_121_txt_path), testfile_121_txt_path);
        verify(observer).modified(key(root_dir_path, testfile_11_xml_path), testfile_11_xml_path);
        verify(observer).modified(key(root_dir_path, testfile_2111_txt_path), testfile_2111_txt_path);
        verify(observer).modified(key(root_dir_path, testfile_211_txt_path), testfile_211_txt_path);
        verify(observer).modified(key(root_dir_path, testfile_221_txt_path), testfile_221_txt_path);
        verify(observer).modified(key(root_dir_path, testfile_21_xml_path), testfile_21_xml_path);
        verify(observer).modified(key(root_dir_path, testfile_txt_path), testfile_txt_path);
        verifyNoMoreInteractions(observer);
    }

    @Test
    public void directoriesDiscardedAfterRelocate() throws Exception {
        when(update.hasChanged()).thenReturn(true);
        informDiscard(root_dir_path);
        deleteDirectory(subdir_11_path);
        deleteDirectory(subdir_2_path);
        informModified(root_dir_path);

        diff.finalizeRelocation();
        verify(observer).discard(key(root_dir_path, testfile_1111_txt_path));
        verify(observer).discard(key(root_dir_path, testfile_111_txt_path));
        verify(observer).discard(key(root_dir_path, testfile_2111_txt_path));
        verify(observer).discard(key(root_dir_path, testfile_211_txt_path));
        verify(observer).discard(key(root_dir_path, testfile_221_txt_path));
        verify(observer).discard(key(root_dir_path, testfile_21_xml_path));
        verify(observer).modified(key(root_dir_path, testfile_121_txt_path), testfile_121_txt_path);
        verify(observer).modified(key(root_dir_path, testfile_11_xml_path), testfile_11_xml_path);
        verify(observer).modified(key(root_dir_path, testfile_txt_path), testfile_txt_path);
        verifyNoMoreInteractions(observer);
    }


    @Test
    public void filesDiscardedAfterRelocate() throws Exception {
        when(update.hasChanged()).thenReturn(true);
        informDiscard(root_dir_path);

        delete(testfile_1111_txt_path);
        delete(testfile_121_txt_path);
        delete(testfile_221_txt_path);
        delete(testfile_txt_path);

        informModified(root_dir_path);

        diff.finalizeRelocation();
        verify(observer).discard(key(root_dir_path, testfile_1111_txt_path));
        verify(observer).discard(key(root_dir_path, testfile_121_txt_path));
        verify(observer).discard(key(root_dir_path, testfile_221_txt_path));
        verify(observer).discard(key(root_dir_path, testfile_txt_path));
        verify(observer).modified(key(root_dir_path, testfile_111_txt_path), testfile_111_txt_path);
        verify(observer).modified(key(root_dir_path, testfile_11_xml_path), testfile_11_xml_path);
        verify(observer).modified(key(root_dir_path, testfile_2111_txt_path), testfile_2111_txt_path);
        verify(observer).modified(key(root_dir_path, testfile_211_txt_path), testfile_211_txt_path);
        verify(observer).modified(key(root_dir_path, testfile_21_xml_path), testfile_21_xml_path);
        verifyNoMoreInteractions(observer);
    }


    @Test
    public void doOnlyModifyThoseWhichHaveChanged() throws Exception {
        final Resource resource_testfile_1111_txt = mock(Resource.class);
        final Resource resource_testfile_121_txt = mock(Resource.class);
        final Resource resource_testfile_211_txt = mock(Resource.class);
        final Resource resource_testfile_txt = mock(Resource.class);

        when(subdir_111.getResource(testfile_1111_txt_path)).thenReturn(resource_testfile_1111_txt);
        when(subdir_12.getResource(testfile_121_txt_path)).thenReturn(resource_testfile_121_txt);
        when(subdir_21.getResource(testfile_211_txt_path)).thenReturn(resource_testfile_211_txt);
        when(root_dir.getResource(testfile_txt_path)).thenReturn(resource_testfile_txt);

        final Update update_testfile_1111_txt = mock(Update.class);
        final Update update_testfile_121_txt = mock(Update.class);
        final Update update_testfile_211_txt = mock(Update.class);
        final Update update_testfile_txt = mock(Update.class);

        setupUpdate(resource_testfile_1111_txt, update_testfile_1111_txt, false);
        setupUpdate(resource_testfile_121_txt, update_testfile_121_txt, false);
        setupUpdate(resource_testfile_211_txt, update_testfile_211_txt, false);
        setupUpdate(resource_testfile_txt, update_testfile_txt, false);


        informDiscard(root_dir_path);
        informModified(root_dir_path);

        diff.finalizeRelocation();
        verify(observer).modified(key(root_dir_path, testfile_111_txt_path), testfile_111_txt_path);
        verify(observer).modified(key(root_dir_path, testfile_11_xml_path), testfile_11_xml_path);
        verify(observer).modified(key(root_dir_path, testfile_2111_txt_path), testfile_2111_txt_path);
        verify(observer).modified(key(root_dir_path, testfile_221_txt_path), testfile_221_txt_path);
        verify(observer).modified(key(root_dir_path, testfile_21_xml_path), testfile_21_xml_path);
        verifyNoMoreInteractions(observer);
    }

    @Test
    public void noResourcesRegisteredForDirectories() throws Exception {
        when(fs.getDirectory(root_dir_path)).thenReturn(null);
        when(fs.getDirectory(subdir_1_path)).thenReturn(null);
        when(fs.getDirectory(subdir_11_path)).thenReturn(null);
        when(fs.getDirectory(subdir_111_path)).thenReturn(null);
        when(fs.getDirectory(subdir_12_path)).thenReturn(null);
        when(fs.getDirectory(subdir_2_path)).thenReturn(null);
        when(fs.getDirectory(subdir_21_path)).thenReturn(null);
        when(fs.getDirectory(subdir_211_path)).thenReturn(null);
        when(fs.getDirectory(subdir_22_path)).thenReturn(null);

        informDiscard(root_dir_path);
        informModified(root_dir_path);

        // This should not cause an exception
        diff.finalizeRelocation();
        verifyNoMoreInteractions(observer);
    }


}
