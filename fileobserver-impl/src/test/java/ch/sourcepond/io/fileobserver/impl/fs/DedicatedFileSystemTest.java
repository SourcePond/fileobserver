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

import ch.sourcepond.io.fileobserver.api.FileObserver;
import ch.sourcepond.io.fileobserver.impl.directory.Directory;
import ch.sourcepond.io.fileobserver.impl.directory.DirectoryFactory;
import ch.sourcepond.io.fileobserver.impl.directory.RootDirectory;
import ch.sourcepond.io.fileobserver.impl.observer.ObserverDispatcher;
import ch.sourcepond.io.fileobserver.spi.WatchedDirectory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.Path;
import java.nio.file.WatchKey;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 */
public class DedicatedFileSystemTest {
    private static final Object DIRECTORY_KEY_1 = "dirKey1";
    private static final Object DIRECTORY_KEY_2 = "dirKey2";
    private final ConcurrentMap<Path, Directory> dirs = new ConcurrentHashMap<>();
    private final PathChangeHandler pathChangeHandler = mock(PathChangeHandler.class);
    private final ObserverDispatcher dispatcher = mock(ObserverDispatcher.class);
    private final DirectoryFactory directoryFactory = mock(DirectoryFactory.class);
    private final WatchedDirectory watchedDirectory1 = mock(WatchedDirectory.class);
    private final WatchedDirectory watchedDirectory2 = mock(WatchedDirectory.class);
    private final RootDirectory rootDir1 = mock(RootDirectory.class);
    private final RootDirectory rootDir2 = mock(RootDirectory.class);
    private final DirectoryRebase rebase = mock(DirectoryRebase.class);
    private final FileObserver observer = mock(FileObserver.class);
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
        fs = new DedicatedFileSystem(directoryFactory, wrapper, rebase, dispatcher, pathChangeHandler, dirs);
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
        fs.registerRootDirectory(mock(WatchedDirectory.class));
    }

    @Test
    public void directoryWithSamePathAlreadyRegistered() throws IOException {
        when(watchedDirectory2.getDirectory()).thenReturn(rootDirPath1);
        fs.registerRootDirectory(watchedDirectory1);
        fs.registerRootDirectory(watchedDirectory2);

        final InOrder order = inOrder(pathChangeHandler, rootDir1);
        order.verify(pathChangeHandler).rootAdded(rootDir1);
        order.verify(rootDir1).addWatchedDirectory(watchedDirectory2);
        order.verifyNoMoreInteractions();
    }

    @Test
    public void registerRootDirectory() throws IOException {
        fs.registerRootDirectory(watchedDirectory1);
        assertSame(rootDir1, fs.getDirectory(rootDirPath1));
        final InOrder order = inOrder(rebase, pathChangeHandler, rootDir1);
        order.verify(rebase).rebaseExistingRootDirectories(rootDir1);
        order.verify(pathChangeHandler).rootAdded(rootDir1);
        order.verify(rootDir1).addWatchedDirectory(watchedDirectory1);
        order.verifyNoMoreInteractions();
    }

    @Test(expected = NullPointerException.class)
    public void insureDirectoryDirectoryCannotBeNullDuringUnregistration() throws IOException {
        final WatchedDirectory invalid = mock(WatchedDirectory.class);
        when(invalid.getKey()).thenReturn(DIRECTORY_KEY_1);
        fs.unregisterRootDirectory(null, mock(WatchedDirectory.class));
    }

    @Test
    public void noExceptionWhenUnknownDirectoryIsBeingUnregistered() throws IOException {
        final WatchedDirectory unknown = mock(WatchedDirectory.class);
        final Path path = mock(Path.class);
        when(unknown.getKey()).thenReturn(DIRECTORY_KEY_1);
        when(unknown.getDirectory()).thenReturn(path);

        // Should not cause an exception
        fs.unregisterRootDirectory(path, unknown);
    }

    @Test
    public void unregisterRootDirectoryStillKeysAvailable() throws IOException {
        when(rootDir1.hasKeys()).thenReturn(true);
        fs.registerRootDirectory(watchedDirectory1);
        verify(rootDir1).addWatchedDirectory(watchedDirectory1);
        fs.unregisterRootDirectory(rootDirPath1, watchedDirectory1);

        final InOrder order = inOrder(rootDir1, rebase);
        order.verify(rootDir1).removeWatchedDirectory(watchedDirectory1);
        order.verify(rootDir1).hasKeys();
        order.verifyNoMoreInteractions();
    }

    @Test
    public void unregisterRootDirectoryAllKeysRemoved() throws IOException {
        fs.registerRootDirectory(watchedDirectory1);
        verify(rootDir1).addWatchedDirectory(watchedDirectory1);
        fs.unregisterRootDirectory(rootDirPath1, watchedDirectory1);

        final InOrder order = inOrder(rootDir1, rebase);
        order.verify(rootDir1).removeWatchedDirectory(watchedDirectory1);
        order.verify(rootDir1).hasKeys();
        order.verify(rebase).cancelAndRebaseDiscardedDirectory(rootDir1);
        order.verifyNoMoreInteractions();
    }

    @Test
    public void close() {
        dirs.put(rootDirPath1, rootDir1);
        fs.close();
        verify(wrapper, timeout(2000)).close();
        assertTrue(dirs.isEmpty());
        verify(pathChangeHandler).removeFileSystem(fs);
    }


    @Test
    public void closeAndRemoveFsWhenWatchServiceClosed() throws Exception {
        final ClosedWatchServiceException expected = new ClosedWatchServiceException();
        fs.start();
        doThrow(expected).when(wrapper).take();
        close();
    }

    @Test
    public void destinationChangedDirectoryNotRegistered() throws IOException {
        final Path previous = mock(Path.class);

        // This should not cause an exception
        fs.destinationChanged(watchedDirectory1, previous);
    }

    @Test
    public void destinationChanged() throws IOException {
        final Closeable diff = mock(Closeable.class);
        when(dispatcher.openDiffHandler(fs)).thenReturn(diff);

        fs.registerRootDirectory(watchedDirectory1);
        fs.destinationChanged(watchedDirectory1, rootDirPath1);

        final InOrder order = inOrder(rootDir1, pathChangeHandler, diff);
        order.verify(rootDir1).removeWatchedDirectory(watchedDirectory1);
        order.verify(rootDir1).addWatchedDirectory(watchedDirectory1);
        order.verify(diff).close();
    }
}
