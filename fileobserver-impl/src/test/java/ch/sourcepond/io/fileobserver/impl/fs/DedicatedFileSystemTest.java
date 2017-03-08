package ch.sourcepond.io.fileobserver.impl.fs;

import ch.sourcepond.io.fileobserver.api.FileObserver;
import ch.sourcepond.io.fileobserver.impl.ExecutorServices;
import ch.sourcepond.io.fileobserver.impl.directory.Directory;
import ch.sourcepond.io.fileobserver.impl.directory.DirectoryFactory;
import ch.sourcepond.io.fileobserver.impl.directory.RootDirectory;
import ch.sourcepond.io.fileobserver.spi.WatchedDirectory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.WatchKey;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;

import static java.util.Arrays.asList;
import static java.util.concurrent.Executors.newCachedThreadPool;
import static org.mockito.Mockito.*;

/**
 * Created by rolandhauser on 06.03.17.
 */
public class DedicatedFileSystemTest {
    private static final Object DIRECTORY_KEY_1 = "dirKey1";
    private static final Object DIRECTORY_KEY_2 = "dirKey2";
    private final ConcurrentMap<Path, Directory> dirs = new ConcurrentHashMap<>();
    private final DirectoryRegistrationWalker walker = mock(DirectoryRegistrationWalker.class);
    private final ExecutorServices executors = mock(ExecutorServices.class);
    private final ExecutorService executor = newCachedThreadPool();
    private final FileObserver observer = mock(FileObserver.class);
    private final Collection<FileObserver> observers = asList(observer);
    private final DirectoryFactory directoryFactory = mock(DirectoryFactory.class);
    private final WatchedDirectory watchedDirectory1 = mock(WatchedDirectory.class);
    private final WatchedDirectory watchedDirectory2 = mock(WatchedDirectory.class);
    private final RootDirectory rootDir = mock(RootDirectory.class);
    private final DirectoryRebase rebase = mock(DirectoryRebase.class);
    private final Path rootDirPath = mock(Path.class);
    private final WatchKey rootWatchKey = mock(WatchKey.class);
    private final WatchServiceWrapper wrapper = mock(WatchServiceWrapper.class);
    private DedicatedFileSystem fs;

    @Before
    public void setup() throws IOException {
        // Setup watched-rootDirPath
        when(watchedDirectory1.getKey()).thenReturn(DIRECTORY_KEY_1);
        when(watchedDirectory1.getDirectory()).thenReturn(rootDirPath);
        when(watchedDirectory2.getKey()).thenReturn(DIRECTORY_KEY_2);

        // Setup watch-key
        when(wrapper.register(rootDirPath)).thenReturn(rootWatchKey);

        // Setup directories
        when(directoryFactory.newRoot(rootWatchKey)).thenReturn(rootDir);
        when(rootDir.getPath()).thenReturn(rootDirPath);

        // Setup fs
        when(executors.getDirectoryWalkerExecutor()).thenReturn(executor);
        fs = new DedicatedFileSystem(executors, directoryFactory, wrapper, rebase, walker, dirs);
    }

    @After
    public void tearDown() {
        executor.shutdown();
    }

    @Test(expected = NullPointerException.class)
    public void insureDirectoryKeyCannotBeNullDuringRegistration() throws IOException{
        fs.registerRootDirectory(mock(WatchedDirectory.class), observers);
    }

    /**
     *
     */
    @Test(expected = IllegalArgumentException.class)
    public void registeringSameKeyIsNotAllowed() throws IOException {
        fs.registerRootDirectory(watchedDirectory1, observers);

        // This should cause an exception
        fs.registerRootDirectory(watchedDirectory1, observers);
    }

    @Test
    public void directoryWithSamePathAlreadyRegistered() throws IOException {
        when(watchedDirectory2.getDirectory()).thenReturn(rootDirPath);
        fs.registerRootDirectory(watchedDirectory1, observers);
        fs.registerRootDirectory(watchedDirectory2, observers);

        final InOrder order = inOrder(walker, rootDir);
        order.verify(walker).rootAdded(rootDir, observers);
        order.verify(rootDir).addDirectoryKey(DIRECTORY_KEY_2);
        order.verifyNoMoreInteractions();
    }

    @Test
    public void registerRootDirectory() throws IOException {
        fs.registerRootDirectory(watchedDirectory1, observers);
        final InOrder order = inOrder(rebase, walker, rootDir);
        order.verify(rebase).rebaseExistingRootDirectories(rootDir);
        order.verify(walker).rootAdded(rootDir, observers);
        order.verify(rootDir).addDirectoryKey(DIRECTORY_KEY_1);
        order.verifyNoMoreInteractions();
    }

    @Test(expected = NullPointerException.class)
    public void insureDirectoryKeyCannotBeNullDuringUnregistration() throws IOException{
        fs.unregisterRootDirectory(mock(WatchedDirectory.class), observers);
    }

    @Test(expected = NullPointerException.class)
    public void insureDirectoryDirectoryCannotBeNullDuringUnregistration() throws IOException{
        final WatchedDirectory invalid = mock(WatchedDirectory.class);
        when(invalid.getKey()).thenReturn(DIRECTORY_KEY_1);
        fs.unregisterRootDirectory(mock(WatchedDirectory.class), observers);
    }

    @Test
    public void noExceptionWhenUnknownDirectoryIsBeingUnregistered() throws IOException{
        final WatchedDirectory unknown = mock(WatchedDirectory.class);
        final Path path = mock(Path.class);
        when(unknown.getKey()).thenReturn(DIRECTORY_KEY_1);
        when(unknown.getDirectory()).thenReturn(path);

        // Should not cause an exception
        fs.unregisterRootDirectory(unknown, observers);
    }

    @Test
    public void unregisterRootDirectoryStillKeysAvailable() throws IOException {
        when(rootDir.hasKeys()).thenReturn(true);
        fs.registerRootDirectory(watchedDirectory1, observers);
        verify(rootDir).addDirectoryKey(DIRECTORY_KEY_1);
        fs.unregisterRootDirectory(watchedDirectory1, observers);

        final InOrder order = inOrder(rootDir, rebase);
        order.verify(rootDir).removeDirectoryKey(DIRECTORY_KEY_1, observers);
        order.verify(rootDir).hasKeys();
        order.verifyNoMoreInteractions();
    }

    @Test
    public void unregisterRootDirectoryAllKeysRemoved() throws IOException {
        fs.registerRootDirectory(watchedDirectory1, observers);
        verify(rootDir).addDirectoryKey(DIRECTORY_KEY_1);
        fs.unregisterRootDirectory(watchedDirectory1, observers);

        final InOrder order = inOrder(rootDir, rebase);
        order.verify(rootDir).removeDirectoryKey(DIRECTORY_KEY_1, observers);
        order.verify(rootDir).hasKeys();
        order.verify(rebase).cancelAndRebaseDiscardedDirectory(rootDir);
        order.verifyNoMoreInteractions();
    }
}
