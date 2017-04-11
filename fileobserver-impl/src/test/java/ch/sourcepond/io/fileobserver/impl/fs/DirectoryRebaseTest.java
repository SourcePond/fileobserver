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
import ch.sourcepond.io.fileobserver.impl.CopyResourcesTest;
import ch.sourcepond.io.fileobserver.impl.directory.Directory;
import ch.sourcepond.io.fileobserver.impl.directory.DirectoryFactory;
import ch.sourcepond.io.fileobserver.impl.directory.RootDirectory;
import ch.sourcepond.io.fileobserver.impl.directory.SubDirectory;
import ch.sourcepond.io.fileobserver.impl.filekey.DefaultFileKeyFactory;
import ch.sourcepond.io.fileobserver.impl.observer.ObserverDispatcher;
import ch.sourcepond.io.fileobserver.spi.WatchedDirectory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;

import static java.nio.file.FileSystems.getDefault;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 */
public class DirectoryRebaseTest extends CopyResourcesTest {
    private static final String DIRECTORY_KEY = "directoryKey";
    private final ConcurrentMap<Path, Directory> dirs = new ConcurrentHashMap<>();
    private final ResourcesFactory resourcesFactory = mock(ResourcesFactory.class);
    private final ExecutorService directoryWalkerExecutor = newSingleThreadExecutor();
    private final ExecutorService observerExecutor = newSingleThreadExecutor();
    private final WatchedDirectory watchedDirectory = mock(WatchedDirectory.class);
    private final Config config = mock(Config.class);
    private final ObserverDispatcher dispatcher = mock(ObserverDispatcher.class);
    private final DirectoryFactory directoryFactory = new DirectoryFactory(
            new DefaultFileKeyFactory(), dispatcher);
    private WatchServiceWrapper wrapper;
    private Directory dir;
    private Directory dir_1;
    private Directory dir_111;
    private Directory dir_2;
    private Directory dir_211;
    private DirectoryRebase rebase;

    @Before
    public void setupDirectories() throws IOException {
        when(watchedDirectory.getKey()).thenReturn(DIRECTORY_KEY);
        wrapper = new WatchServiceWrapper(getDefault());
        directoryFactory.setConfig(config);
        directoryFactory.setObserverExecutor(observerExecutor);
        directoryFactory.setResourcesFactory(resourcesFactory);
        directoryFactory.setDirectoryWalkerExecutor(directoryWalkerExecutor);
        dir = directoryFactory.newRoot(wrapper.register(root_dir_path));
        dir_1 = directoryFactory.newRoot(wrapper.register(subdir_1_path));
        dir_111 = directoryFactory.newRoot(wrapper.register(subdir_111_path));
        dir_2 = directoryFactory.newRoot(wrapper.register(subdir_2_path));
        dir_211 = directoryFactory.newRoot(wrapper.register(subdir_211_path));
        rebase = new DirectoryRebase(directoryFactory, wrapper, dirs);
    }

    @After
    public void shutdownExecutor() {
        wrapper.close();
        directoryWalkerExecutor.shutdown();
        observerExecutor.shutdown();
    }

    @Test
    public void rebaseDirectoryWithNonExistingDirectoriesInBetween() throws IOException {
        // For the test, dir_111 must be a root directory
        dir_111 = directoryFactory.newRoot(wrapper.register(subdir_111_path));
        // For the test, dir_12 must be a root directory
        dir_211 = directoryFactory.newRoot(wrapper.register(subdir_211_path));

        dirs.put(subdir_111_path, dir_111);
        dirs.put(subdir_211_path, dir_211);

        rebase.rebaseExistingRootDirectories(dir);
        assertEquals(7, dirs.size());

        // Firstly, check the new root directory
        final Directory expectedRoot = dirs.get(root_dir_path);
        assertNotNull(expectedRoot);
        assertTrue(expectedRoot.isRoot());

        // Check, the rebased former root dir_111
        final Directory rebasedDir_111 = dirs.get(subdir_111_path);
        assertNotSame(dir_111, rebasedDir_111);
        assertFalse(rebasedDir_111.isRoot());

        final Directory dir_11 = dirs.get(subdir_11_path);
        assertNotNull(dir_11);
        assertFalse(dir_11.isRoot());

        final Directory dir_1 = dirs.get(subdir_1_path);
        assertNotNull(dir_1);
        assertFalse(dir_1.isRoot());

        assertTrue(expectedRoot.isDirectParentOf(dir_1));

        // Check, the rebased former root dir_211
        final Directory rebasedDir_211 = dirs.get(subdir_211_path);
        assertNotSame(dir_211, rebasedDir_211);
        assertFalse(rebasedDir_211.isRoot());

        final Directory dir_21 = dirs.get(subdir_21_path);
        assertNotNull(dir_21);
        assertFalse(dir_21.isRoot());

        final Directory dir_2 = dirs.get(subdir_2_path);
        assertNotNull(dir_2);
        assertFalse(dir_2.isRoot());

        assertTrue(expectedRoot.isDirectParentOf(dir_2));
    }

    @Test
    public void rebaseExistingRootDirectoriesWhichAreDirectChildren() throws IOException {
        dirs.put(subdir_1_path, dir_1);
        dirs.put(subdir_2_path, dir_2);
        rebase.rebaseExistingRootDirectories(dir);

        assertEquals(3, dirs.size());

        final Directory expectedRoot = dirs.get(root_dir_path);
        assertNotNull(expectedRoot);
        assertTrue(expectedRoot.isRoot());

        final Directory rebasedExistingRoot1 = dirs.get(subdir_1_path);
        assertNotSame(dir_1, rebasedExistingRoot1);
        assertFalse(rebasedExistingRoot1.isRoot());
        assertTrue(expectedRoot.isDirectParentOf(rebasedExistingRoot1));

        final Directory rebasedExistingRoot2 = dirs.get(subdir_1_path);
        assertNotSame(dir_2, rebasedExistingRoot2);
        assertFalse(rebasedExistingRoot2.isRoot());
        assertTrue(expectedRoot.isDirectParentOf(rebasedExistingRoot2));
    }

    @Test
    public void insureParentsOfDirectSubDirectoriesChangedAfterRebase() throws IOException {
        dirs.put(subdir_1_path, dir_1);
        dirs.put(subdir_2_path, dir_2);
        final Directory dir_11 = directoryFactory.newBranch(dir_1, wrapper.register(subdir_11_path));
        dirs.put(subdir_11_path, dir_11);
        final Directory dir_21 = directoryFactory.newBranch(dir_2, wrapper.register(subdir_21_path));
        dirs.put(subdir_21_path, dir_21);

        rebase.rebaseExistingRootDirectories(dir);
        assertEquals(5, dirs.size());

        final Directory expectedRoot = dirs.get(root_dir_path);
        assertNotNull(expectedRoot);
        assertTrue(expectedRoot.isRoot());

        final Directory rebasedDir_1 = dirs.get(subdir_1_path);
        assertNotSame(dir_1, rebasedDir_1);
        final Directory rebasedDir_2 = dirs.get(subdir_2_path);
        assertNotSame(dir_2, rebasedDir_2);
        assertSame(dir_11, dirs.get(subdir_11_path));
        assertSame(dir_21, dirs.get(subdir_21_path));

        assertTrue(expectedRoot.isDirectParentOf(rebasedDir_1));
        assertTrue(expectedRoot.isDirectParentOf(rebasedDir_2));
        assertTrue(rebasedDir_1.isDirectParentOf(dir_11));
        assertTrue(rebasedDir_2.isDirectParentOf(dir_21));
    }

    @Test
    public void rebaseExistingChildDirectoriesAfterRootDiscard() throws IOException {
        // For the test, dir_11 must be a root directory, and, it must contain a key
        final Directory dir_11 = directoryFactory.newRoot(wrapper.register(subdir_11_path));
        ((RootDirectory)dir_11).addWatchedDirectory(watchedDirectory);
        dirs.put(subdir_11_path, dir_11);
        // For the test, dir_12 must be a root directory, and, it must contain a key
        final Directory dir_21 = directoryFactory.newRoot(wrapper.register(subdir_21_path));
        ((RootDirectory)dir_21).addWatchedDirectory(watchedDirectory);
        dirs.put(subdir_21_path, dir_21);


        dir_111 = directoryFactory.newBranch(dir_11, wrapper.register(subdir_111_path));
        dirs.put(subdir_111_path, dir_111);
        dir_211 = directoryFactory.newBranch(dir_21, wrapper.register(subdir_211_path));
        dirs.put(subdir_211_path, dir_211);

        assertSame(dir_11, ((SubDirectory)dir_111).getParent());
        assertSame(dir_21, ((SubDirectory)dir_211).getParent());
        rebase.rebaseExistingRootDirectories(dir);

        rebase.cancelAndRebaseDiscardedDirectory(dir);

        assertEquals(4, dirs.size());
        final Directory rebasedRoot11 = dirs.get(subdir_11_path);
        assertNotNull(rebasedRoot11);
        assertTrue(rebasedRoot11.isRoot());
        assertNotSame(dir_11, rebasedRoot11);

        final Directory rebasedRoot21 = dirs.get(subdir_21_path);
        assertNotNull(rebasedRoot21);
        assertTrue(rebasedRoot21.isRoot());
        assertNotSame(dir_21, rebasedRoot21);

        assertSame(dir_111, dirs.get(subdir_111_path));
        assertSame(dir_211, dirs.get(subdir_211_path));
        assertSame(rebasedRoot11, ((SubDirectory)dir_111).getParent());
        assertSame(rebasedRoot21, ((SubDirectory)dir_211).getParent());
    }

    /**
     *
     */
    @Test
    public void insureFormerRootDirectoriesAreConvertedBackAfterRootDiscard() throws IOException {
        // For the test, dir_111 must be a root directory, and, it must contain a key
        dir_111 = directoryFactory.newRoot(wrapper.register(subdir_111_path));
        ((RootDirectory)dir_111).addWatchedDirectory(watchedDirectory);
        // For the test, dir_211 must be a root directory, and, it must contain a key
        dir_211 = directoryFactory.newRoot(wrapper.register(subdir_211_path));
        ((RootDirectory)dir_211).addWatchedDirectory(watchedDirectory);

        dirs.put(subdir_111_path, dir_111);
        dirs.put(subdir_211_path, dir_211);

        rebase.rebaseExistingRootDirectories(dir);
        rebase.cancelAndRebaseDiscardedDirectory(dir);

        assertEquals(2, dirs.size());
        final Directory rebasedRoot1 = dirs.get(subdir_111_path);
        assertNotNull(rebasedRoot1);
        assertTrue(rebasedRoot1.isRoot());
        assertNotSame(dir_111, rebasedRoot1);

        final Directory rebasedRoot2 = dirs.get(subdir_211_path);
        assertNotNull(rebasedRoot2);
        assertTrue(rebasedRoot2.isRoot());
        assertNotSame(dir_211, rebasedRoot2);
    }

    @Test
    public void ignoreAlreadyAddedRootDuringExistingRootsCollection() throws IOException {

    }
}
