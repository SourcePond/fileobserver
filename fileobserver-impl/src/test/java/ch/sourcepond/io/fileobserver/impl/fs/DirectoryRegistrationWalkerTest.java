package ch.sourcepond.io.fileobserver.impl.fs;

import ch.sourcepond.io.fileobserver.api.FileObserver;
import ch.sourcepond.io.fileobserver.impl.CopyResourcesTest;
import ch.sourcepond.io.fileobserver.impl.ExecutorServices;
import ch.sourcepond.io.fileobserver.impl.directory.Directory;
import ch.sourcepond.io.fileobserver.impl.directory.DirectoryFactory;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.WatchKey;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.*;

/**
 * Created by rolandhauser on 07.03.17.
 */
public class DirectoryRegistrationWalkerTest extends CopyResourcesTest {
    private static final String ANY_MESSAGE = "anyMessage";
    private final Logger logger = mock(Logger.class);
    private final ExecutorService executor = newSingleThreadExecutor();
    private final ExecutorServices executorServices = mock(ExecutorServices.class);
    private final WatchServiceWrapper wrapper = mock(WatchServiceWrapper.class);
    private final DirectoryFactory directoryFactory = mock(DirectoryFactory.class);
    private final ConcurrentMap<Path, Directory> dirs = new ConcurrentHashMap<>();
    private final Collection<FileObserver> observers = mock(Collection.class);
    private final WatchKey root_dir_watchKey = mock(WatchKey.class);
    private final WatchKey subdir_1_watchKey = mock(WatchKey.class);
    private final WatchKey subdir_11_watchKey = mock(WatchKey.class);
    private final WatchKey subdir_111_watchKey = mock(WatchKey.class);
    private final WatchKey subdir_12_watchKey = mock(WatchKey.class);
    private final WatchKey subdir_2_watchKey = mock(WatchKey.class);
    private final WatchKey subdir_21_watchKey = mock(WatchKey.class);
    private final WatchKey subdir_211_watchKey = mock(WatchKey.class);
    private final WatchKey subdir_22_watchKey = mock(WatchKey.class);
    private final Directory root_dir = mock(Directory.class);
    private final Directory subdir_1 = mock(Directory.class);
    private final Directory subdir_11 = mock(Directory.class);
    private final Directory subdir_111 = mock(Directory.class);
    private final Directory subdir_12 = mock(Directory.class);
    private final Directory subdir_2 = mock(Directory.class);
    private final Directory subdir_21 = mock(Directory.class);
    private final Directory subdir_211 = mock(Directory.class);
    private final Directory subdir_22 = mock(Directory.class);
    private DirectoryRegistrationWalker walker = new DirectoryRegistrationWalker(
            executorServices,
            wrapper,
            directoryFactory,
            dirs);

    @Before
    public void setupDirectories() throws IOException {
        // Root directory is already registered before the walker is
        // executed, so simulate this here.
        dirs.put(root_dir_path, root_dir);

        when(executorServices.getDirectoryWalkerExecutor()).thenReturn(executor);

        when(wrapper.register(subdir_1_path)).thenReturn(subdir_1_watchKey);
        when(wrapper.register(subdir_11_path)).thenReturn(subdir_11_watchKey);
        when(wrapper.register(subdir_111_path)).thenReturn(subdir_111_watchKey);
        when(wrapper.register(subdir_12_path)).thenReturn(subdir_12_watchKey);
        when(wrapper.register(subdir_2_path)).thenReturn(subdir_2_watchKey);
        when(wrapper.register(subdir_21_path)).thenReturn(subdir_21_watchKey);
        when(wrapper.register(subdir_211_path)).thenReturn(subdir_211_watchKey);
        when(wrapper.register(subdir_22_path)).thenReturn(subdir_22_watchKey);

        when(directoryFactory.newBranch(root_dir, subdir_1_watchKey)).thenReturn(subdir_1);
        when(directoryFactory.newBranch(subdir_1, subdir_11_watchKey)).thenReturn(subdir_11);
        when(directoryFactory.newBranch(subdir_11, subdir_111_watchKey)).thenReturn(subdir_111);
        when(directoryFactory.newBranch(subdir_1, subdir_12_watchKey)).thenReturn(subdir_12);
        when(directoryFactory.newBranch(root_dir, subdir_2_watchKey)).thenReturn(subdir_2);
        when(directoryFactory.newBranch(subdir_2, subdir_21_watchKey)).thenReturn(subdir_21);
        when(directoryFactory.newBranch(subdir_21, subdir_211_watchKey)).thenReturn(subdir_211);
        when(directoryFactory.newBranch(subdir_2, subdir_22_watchKey)).thenReturn(subdir_22);
    }

    private void verifyDirectoryWalk(final Directory pNewRootOrNull) throws IOException {
        verify(subdir_111, timeout(200)).informIfChanged(pNewRootOrNull, observers, testfile_1111_txt_path);
        verify(subdir_11, timeout(200)).informIfChanged(pNewRootOrNull, observers, testfile_111_txt_path);
        verify(subdir_12, timeout(200)).informIfChanged(pNewRootOrNull, observers, testfile_121_txt_path);
        verify(subdir_1, timeout(200)).informIfChanged(pNewRootOrNull, observers, testfile_11_xml_path);
        verify(subdir_211, timeout(200)).informIfChanged(pNewRootOrNull, observers, testfile_2111_txt_path);
        verify(subdir_21, timeout(200)).informIfChanged(pNewRootOrNull, observers, testfile_211_txt_path);
        verify(subdir_22, timeout(200)).informIfChanged(pNewRootOrNull, observers, testfile_221_txt_path);
        verify(subdir_2, timeout(200)).informIfChanged(pNewRootOrNull, observers, testfile_21_xml_path);
        verify(root_dir, timeout(200)).informIfChanged(pNewRootOrNull, observers, testfile_txt_path);
        verifyNoMoreInteractions(subdir_111,
                subdir_11, subdir_12, subdir_1,
                subdir_211, subdir_21, subdir_22,
                subdir_2, root_dir);

        assertEquals(9, dirs.size());
        assertSame(subdir_111, dirs.get(subdir_111_path));
        assertSame(subdir_11, dirs.get(subdir_11_path));
        assertSame(subdir_12, dirs.get(subdir_12_path));
        assertSame(subdir_1, dirs.get(subdir_1_path));
        assertSame(subdir_211, dirs.get(subdir_211_path));
        assertSame(subdir_21, dirs.get(subdir_21_path));
        assertSame(subdir_22, dirs.get(subdir_22_path));
        assertSame(subdir_2, dirs.get(subdir_2_path));
        assertSame(root_dir, dirs.get(root_dir_path));
    }

    /**
     *
     */
    @Test
    public void directoryCreated() throws IOException {
        walker.directoryCreated(root_dir_path, observers);
        verifyDirectoryWalk(null);
    }

    @Test
    public void rootRebased() throws IOException {
        final Directory newRoot = mock(Directory.class);
        when(newRoot.getPath()).thenReturn(root_dir_path);
        walker.rootRebased(newRoot, observers);
        verifyDirectoryWalk(newRoot);
    }

    @Test
    public void logWarnWhenIOExceptionOccurs() throws IOException {
        walker = new DirectoryRegistrationWalker(
                logger,
                executorServices,
                wrapper,
                directoryFactory,
                dirs);
        final IOException expected = new IOException(ANY_MESSAGE);
        doThrow(expected).when(wrapper).register(subdir_11_path);
        walker.directoryCreated(root_dir_path, observers);
        verify(logger, timeout(200)).warn(eq(ANY_MESSAGE), argThat((Throwable th) -> {
            Throwable cause = th.getCause();
            return (cause instanceof UncheckedIOException) && expected == cause.getCause();
        }));
    }

    @Test
    public void logErrorWhenRuntimeExceptionOccurs() throws IOException {
        walker = new DirectoryRegistrationWalker(
                logger,
                executorServices,
                wrapper,
                directoryFactory,
                dirs);
        final RuntimeException expected = new RuntimeException(ANY_MESSAGE);
        doThrow(expected).when(wrapper).register(subdir_11_path);
        walker.directoryCreated(root_dir_path, observers);
        verify(logger, timeout(200)).error(ANY_MESSAGE, expected);
    }
}
