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

import ch.sourcepond.io.checksum.api.CalculationObserver;
import ch.sourcepond.io.checksum.api.Checksum;
import ch.sourcepond.io.checksum.api.Resource;
import ch.sourcepond.io.fileobserver.api.FileKey;
import ch.sourcepond.io.fileobserver.api.FileObserver;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.WatchKey;
import java.util.Collection;

import static ch.sourcepond.io.checksum.api.Algorithm.SHA256;
import static ch.sourcepond.io.fileobserver.impl.directory.Directory.TIMEOUT;
import static java.util.Arrays.asList;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.*;

/**
 *
 */
public class DirectoryTest {
    private static final Object ROOT_DIR_WATCH_KEY = new Object();
    private static final Object SUB_DIR_WATCH_KEY = new Object();
    private final FileObserver observer = mock(FileObserver.class);
    private final Collection<FileObserver> observers = asList(observer);
    private final Path file = mock(Path.class);
    private final Path relativeFileToRoot = mock(Path.class);
    private final Path relativeFileToSubDir = mock(Path.class);
    private final Resource resource = mock(Resource.class);
    private final Checksum checksum1 = mock(Checksum.class);
    private final Checksum checksum2 = mock(Checksum.class);
    private final DirectoryFactory factory = mock(DirectoryFactory.class);
    private final Path rootDirPath = mock(Path.class);
    private final Path subDirPath = mock(Path.class);
    private final WatchKey rootDirWatchKey = mock(WatchKey.class);
    private final WatchKey subDirWatchKey = mock(WatchKey.class);
    private final FileKey rootDirKey = mock(FileKey.class);
    private final FileKey subDirKey = mock(FileKey.class);
    private final RootDirectory rootDir = new RootDirectory(factory, rootDirWatchKey);
    private final SubDirectory subDir = new SubDirectory(rootDir, subDirWatchKey);

    @Before
    public void setup() {
        doAnswer(invocationOnMock -> {
            final Runnable task = invocationOnMock.getArgument(0);
            task.run();
            return  null;
        }).when(factory).execute(notNull());
        when(factory.newResource(SHA256, file)).thenReturn(resource);
        when(rootDirWatchKey.watchable()).thenReturn(rootDirPath);
        when(subDirWatchKey.watchable()).thenReturn(subDirPath);
        when(rootDirPath.relativize(file)).thenReturn(relativeFileToRoot);
        when(subDirPath.relativize(file)).thenReturn(relativeFileToSubDir);
        when(factory.newKey(ROOT_DIR_WATCH_KEY, relativeFileToRoot)).thenReturn(rootDirKey);
        when(factory.newKey(SUB_DIR_WATCH_KEY, relativeFileToSubDir)).thenReturn(subDirKey);
        rootDir.addDirectoryKey(ROOT_DIR_WATCH_KEY);
        subDir.addDirectoryKey(SUB_DIR_WATCH_KEY);
    }

    private void setupChecksumAnswer(final Checksum pChecksum2) {
        doAnswer(invocationOnMock -> {
            final CalculationObserver cobsrv = invocationOnMock.getArgument(1);
            cobsrv.done(checksum1, pChecksum2);
            return null;
        }).when(resource).update(eq(TIMEOUT), notNull());
    }

    /**
     *
     */
    @Test
    public void rootDirInformIfChangedChecksumsEqual() {
        setupChecksumAnswer(checksum1);
        rootDir.informIfChanged(observers, file);
        verifyZeroInteractions(observer);
    }

    /**
     *
     */
    @Test
    public void rootDirInformIfChangedChecksumsDifferent() {
        setupChecksumAnswer(checksum2);
        rootDir.informIfChanged(observers, file);
        verify(observer).modified(rootDirKey, file);
        verifyNoMoreInteractions(observer);
    }

    /**
     *
     */
    @Test
    public void rootDirInformIfChangedChecksumsDifferentButNoKeyRegistered() {
        setupChecksumAnswer(checksum2);
        rootDir.removeDirectoryKey(ROOT_DIR_WATCH_KEY);
        rootDir.informIfChanged(observers, file);
        verifyZeroInteractions(observer);
    }

    /**
     *
     */
    @Test
    public void subDirInformIfChangedChecksumsEqual() {
        setupChecksumAnswer(checksum1);
        subDir.informIfChanged(observers, file);
        verifyZeroInteractions(observer);
    }

    /**
     *
     */
    @Test
    public void subDirInformIfChangedChecksumsDifferent() {
        setupChecksumAnswer(checksum2);
        subDir.informIfChanged(observers, file);
        verify(observer).modified(rootDirKey, file);
        verify(observer).modified(subDirKey, file);
        verifyNoMoreInteractions(observer);
    }

    /**
     *
     */
    @Test
    public void subDirInformIfChangedChecksumsDifferentOnlyRootKeyRegistered() {
        setupChecksumAnswer(checksum2);
        subDir.removeDirectoryKey(SUB_DIR_WATCH_KEY);
        subDir.informIfChanged(observers, file);
        verify(observer).modified(rootDirKey, file);
        verifyNoMoreInteractions(observer);
    }
}
