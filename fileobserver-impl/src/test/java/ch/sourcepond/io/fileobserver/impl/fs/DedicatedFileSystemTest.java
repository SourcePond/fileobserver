package ch.sourcepond.io.fileobserver.impl.fs;

import ch.sourcepond.io.fileobserver.api.FileObserver;
import ch.sourcepond.io.fileobserver.impl.diff.DiffObserverFactory;
import ch.sourcepond.io.fileobserver.impl.directory.Directory;
import ch.sourcepond.io.fileobserver.impl.directory.DirectoryFactory;
import ch.sourcepond.io.fileobserver.impl.directory.RootDirectory;
import ch.sourcepond.io.fileobserver.spi.WatchedDirectory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.Path;
import java.nio.file.WatchKey;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Created by rolandhauser on 06.03.17.
 */
public class DedicatedFileSystemTest {
    private static final Object DIRECTORY_KEY_1 = "dirKey1";
    private static final Object DIRECTORY_KEY_2 = "dirKey2";
    private final ConcurrentMap<Path, Directory> dirs = new ConcurrentHashMap<>();
    private final VirtualRoot virtualRoot = mock(VirtualRoot.class);
    private final DiffObserverFactory diffObserverFactory = mock(DiffObserverFactory.class);
    private final DirectoryRegistrationWalker walker = mock(DirectoryRegistrationWalker.class);
    private final FileObserver observer = mock(FileObserver.class);
    private final Collection<FileObserver> observers = asList(observer);
    private final DirectoryFactory directoryFactory = mock(DirectoryFactory.class);
    private final WatchedDirectory watchedDirectory1 = mock(WatchedDirectory.class);
    private final WatchedDirectory watchedDirectory2 = mock(WatchedDirectory.class);
    private final RootDirectory rootDir1 = mock(RootDirectory.class);
    private final RootDirectory rootDir2 = mock(RootDirectory.class);
    private final DirectoryRebase rebase = mock(DirectoryRebase.class);
    private final Path rootDirPath1 = mock(Path.class);
    private final Path rootDirPath2 = mock(Path.class);
    private final WatchKey rootWatchKey1 = mock(WatchKey.class);
    private final WatchServiceWrapper wrapper = mock(WatchServiceWrapper.class);
    private DedicatedFileSystem fs;

    @Before
    public void setup() throws IOException {
        // Setup watched-rootDirPath1
        when(watchedDirectory1.getKey()).thenReturn(DIRECTORY_KEY_1);
        when(watchedDirectory1.getDirectory()).thenReturn(rootDirPath1);
        when(watchedDirectory2.getKey()).thenReturn(DIRECTORY_KEY_2);
        when(watchedDirectory2.getDirectory()).thenReturn(rootDirPath2);

        // Setup watch-key
        when(wrapper.register(rootDirPath1)).thenReturn(rootWatchKey1);

        // Setup directories
        when(directoryFactory.newRoot(rootWatchKey1)).thenReturn(rootDir1);
        when(rootDir1.getPath()).thenReturn(rootDirPath1);

        // Simulate DirectoryRebase behaviour
        doAnswer(inv -> {
            final Directory newRoot = inv.getArgument(0);
            dirs.put(newRoot.getPath(), newRoot);
            return null;
        }).when(rebase).rebaseExistingRootDirectories(notNull());

        // Setup fs
        fs = new DedicatedFileSystem(virtualRoot, directoryFactory, wrapper, rebase, walker, diffObserverFactory, dirs);
    }

    @Test
    public void forceInform() {
        dirs.put(rootDirPath1, rootDir1);
        dirs.put(rootDirPath2, rootDir2);
        fs.forceInform(observer);
        verify(rootDir1).forceInform(observer);
        verify(rootDir2).forceInform(observer);
    }

    @Test(expected = NullPointerException.class)
    public void insureDirectoryKeyCannotBeNullDuringRegistration() throws IOException {
        fs.registerRootDirectory(mock(WatchedDirectory.class), observers);
    }

    @Test
    public void directoryWithSamePathAlreadyRegistered() throws IOException {
        when(watchedDirectory2.getDirectory()).thenReturn(rootDirPath1);
        fs.registerRootDirectory(watchedDirectory1, observers);
        fs.registerRootDirectory(watchedDirectory2, observers);

        final InOrder order = inOrder(walker, rootDir1);
        order.verify(walker).rootAdded(rootDir1, observers);
        order.verify(rootDir1).addDirectoryKey(DIRECTORY_KEY_2);
        order.verifyNoMoreInteractions();
    }

    @Test
    public void registerRootDirectory() throws IOException {
        fs.registerRootDirectory(watchedDirectory1, observers);
        assertSame(rootDir1, fs.getDirectory(rootDirPath1));
        final InOrder order = inOrder(rebase, walker, rootDir1);
        order.verify(rebase).rebaseExistingRootDirectories(rootDir1);
        order.verify(walker).rootAdded(rootDir1, observers);
        order.verify(rootDir1).addDirectoryKey(DIRECTORY_KEY_1);
        order.verifyNoMoreInteractions();
    }

    @Test(expected = NullPointerException.class)
    public void insureDirectoryKeyCannotBeNullDuringUnregistration() throws IOException {
        fs.unregisterRootDirectory(mock(WatchedDirectory.class), observers);
    }

    @Test(expected = NullPointerException.class)
    public void insureDirectoryDirectoryCannotBeNullDuringUnregistration() throws IOException {
        final WatchedDirectory invalid = mock(WatchedDirectory.class);
        when(invalid.getKey()).thenReturn(DIRECTORY_KEY_1);
        fs.unregisterRootDirectory(mock(WatchedDirectory.class), observers);
    }

    @Test
    public void noExceptionWhenUnknownDirectoryIsBeingUnregistered() throws IOException {
        final WatchedDirectory unknown = mock(WatchedDirectory.class);
        final Path path = mock(Path.class);
        when(unknown.getKey()).thenReturn(DIRECTORY_KEY_1);
        when(unknown.getDirectory()).thenReturn(path);

        // Should not cause an exception
        fs.unregisterRootDirectory(unknown, observers);
    }

    @Test
    public void unregisterRootDirectoryStillKeysAvailable() throws IOException {
        when(rootDir1.hasKeys()).thenReturn(true);
        fs.registerRootDirectory(watchedDirectory1, observers);
        verify(rootDir1).addDirectoryKey(DIRECTORY_KEY_1);
        fs.unregisterRootDirectory(watchedDirectory1, observers);

        final InOrder order = inOrder(rootDir1, rebase);
        order.verify(rootDir1).removeDirectoryKey(DIRECTORY_KEY_1, observers);
        order.verify(rootDir1).hasKeys();
        order.verifyNoMoreInteractions();
    }

    @Test
    public void unregisterRootDirectoryAllKeysRemoved() throws IOException {
        fs.registerRootDirectory(watchedDirectory1, observers);
        verify(rootDir1).addDirectoryKey(DIRECTORY_KEY_1);
        fs.unregisterRootDirectory(watchedDirectory1, observers);

        final InOrder order = inOrder(rootDir1, rebase);
        order.verify(rootDir1).removeDirectoryKey(DIRECTORY_KEY_1, observers);
        order.verify(rootDir1).hasKeys();
        order.verify(rebase).cancelAndRebaseDiscardedDirectory(rootDir1);
        order.verifyNoMoreInteractions();
    }

    @Test
    public void directoryCreated() {
        fs.directoryCreated(rootDirPath1, observers);
        verify(walker).directoryCreated(rootDirPath1, observers);
    }

    @Test
    public void unknownDirectoryDiscarded() {
        // This should not be removed
        dirs.put(rootDirPath2, rootDir2);
        assertFalse(fs.directoryDiscarded(observers, rootDirPath1));
        assertEquals(1, dirs.size());
        assertTrue(dirs.containsKey(rootDirPath2));
        assertTrue(dirs.containsValue(rootDir2));
    }

    @Test
    public void directoryDiscardedWithMatchingSubDir() {
        // This should not be removed
        dirs.put(rootDirPath2, rootDir2);

        final Path subDirPath = mock(Path.class);
        when(subDirPath.startsWith(rootDirPath1)).thenReturn(true);
        final Directory subDir = mock(Directory.class);
        dirs.put(subDirPath, subDir);
        dirs.put(rootDirPath1, rootDir1);

        assertTrue(fs.directoryDiscarded(observers, rootDirPath1));
        verify(rootDir1).cancelKey();
        verify(subDir).cancelKey();
        verify(rootDir1).informDiscard(observers, rootDirPath1);
        verifyNoMoreInteractions(rootDir1, subDir);
        verifyZeroInteractions(rootDir2);

        assertEquals(1, dirs.size());
        assertTrue(dirs.containsKey(rootDirPath2));
        assertTrue(dirs.containsValue(rootDir2));
    }

    @Test
    public void close() {
        dirs.put(rootDirPath1, rootDir1);
        fs.close();
        verify(wrapper, timeout(2000)).close();
        assertTrue(dirs.isEmpty());
        verify(virtualRoot).removeFileSystem(fs);
    }


    @Test
    public void closeAndRemoveFsWhenWatchServiceClosed() throws Exception {
        final ClosedWatchServiceException expected = new ClosedWatchServiceException();
        fs.start();
        doThrow(expected).when(wrapper).take();
        close();
    }
}
