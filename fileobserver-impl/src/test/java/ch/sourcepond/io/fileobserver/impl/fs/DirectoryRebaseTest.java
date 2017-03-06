package ch.sourcepond.io.fileobserver.impl.fs;

import ch.sourcepond.io.fileobserver.impl.CopyResourcesTest;
import ch.sourcepond.io.fileobserver.impl.ExecutorServices;
import ch.sourcepond.io.fileobserver.impl.directory.Directory;
import ch.sourcepond.io.fileobserver.impl.directory.DirectoryFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;

import static java.nio.file.FileSystems.getDefault;
import static java.util.concurrent.Executors.newCachedThreadPool;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by rolandhauser on 06.03.17.
 */
public class DirectoryRebaseTest extends CopyResourcesTest {
    private final ConcurrentMap<Path, Directory> dirs = new ConcurrentHashMap<>();
    private final ExecutorService executor = newCachedThreadPool();
    private final ExecutorServices executorServices = mock(ExecutorServices.class);
    protected final DirectoryFactory directoryFactory = new DirectoryFactory(executorServices);
    protected WatchServiceRegistrar wsRegistrar;
    protected Directory dir;
    protected Directory dir_1;
    protected Directory dir_111;
    protected Directory dir_2;
    protected Directory dir_211;
    private DirectoryRebase rebase;

    @Before
    public void setupDirectories() throws IOException {
        wsRegistrar = new WatchServiceRegistrar(getDefault().newWatchService());
        dir = directoryFactory.newRoot(wsRegistrar.register(root_dir_path));
        dir_1 = directoryFactory.newRoot(wsRegistrar.register(subdir_1_path));
        dir_111 = directoryFactory.newRoot(wsRegistrar.register(subdir_111_path));
        dir_2 = directoryFactory.newRoot(wsRegistrar.register(subdir_2_path));
        dir_211 = directoryFactory.newRoot(wsRegistrar.register(subdir_211_path));
        when(executorServices.getDirectoryWalkerExecutor()).thenReturn(executor);
        when(executorServices.getObserverExecutor()).thenReturn(executor);
        rebase = new DirectoryRebase(directoryFactory, wsRegistrar);
    }

    @After
    public void shutdownExecutor() {
        wsRegistrar.close();
        executor.shutdown();
    }

    @Test
    public void rebaseDirectoryWithNonExistingDirectoriesInBetween() throws IOException {
        // For the test, dir_111 must be a root directory
        dir_111 = directoryFactory.newRoot(wsRegistrar.register(subdir_111_path));
        // For the test, dir_12 must be a root directory
        dir_211 = directoryFactory.newRoot(wsRegistrar.register(subdir_211_path));

        dirs.put(subdir_111_path, dir_111);
        dirs.put(subdir_211_path, dir_211);

        rebase.rebaseExistingRootDirectories(dir, dirs);
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
        rebase.rebaseExistingRootDirectories(dir, dirs);

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
        final Directory dir_11 = directoryFactory.newBranch(dir_1, wsRegistrar.register(subdir_11_path));
        dirs.put(subdir_11_path, dir_11);
        final Directory dir_21 = directoryFactory.newBranch(dir_2, wsRegistrar.register(subdir_21_path));
        dirs.put(subdir_21_path, dir_21);

        rebase.rebaseExistingRootDirectories(dir,dirs);
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
}
