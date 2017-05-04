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

import ch.sourcepond.io.fileobserver.impl.CopyResourcesTest;
import ch.sourcepond.io.fileobserver.impl.directory.DirectoryFactory;
import ch.sourcepond.io.fileobserver.impl.directory.RootDirectory;
import ch.sourcepond.io.fileobserver.impl.observer.EventDispatcher;
import ch.sourcepond.io.fileobserver.impl.observer.ListenerManager;
import ch.sourcepond.io.fileobserver.spi.WatchedDirectory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.WatchKey;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.Thread.sleep;
import static java.nio.file.Files.delete;
import static java.nio.file.Files.newBufferedWriter;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.util.UUID.randomUUID;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 *
 */
public class DedicatedFileSystemFileChangeTest extends CopyResourcesTest {
    private static final String DIRECTORY_KEY = "getDirectoryKey";
    private static final String NEW_FILE_NAME = "newfile.txt";
    private final WatchedDirectory watchedDirectory = mock(WatchedDirectory.class);
    private final RootDirectory directory = mock(RootDirectory.class);
    private final DirectoryFactory directoryFactory = mock(DirectoryFactory.class);
    private final DirectoryRebase rebase = mock(DirectoryRebase.class);
    private final ListenerManager manager = mock(ListenerManager.class);
    private final EventDispatcher dispatcher = mock(EventDispatcher.class);
    private final PathChangeHandler pathChangeHandler = mock(PathChangeHandler.class);
    private DedicatedFileSystem child;
    private volatile Throwable threadKiller;
    private Path file;
    private WatchServiceWrapper wrapper;
    private WatchKey key;

    @Before
    public void setup() throws Exception {
        when(manager.getDefaultDispatcher()).thenReturn(dispatcher);
        when(watchedDirectory.getDirectory()).thenReturn(root_dir_path);
        when(watchedDirectory.getKey()).thenReturn(DIRECTORY_KEY);
        wrapper = new WatchServiceWrapper(root_dir_path.getFileSystem());
        key = wrapper.register(root_dir_path);
        when(directoryFactory.newRoot(key)).thenReturn(directory);
        child = new DedicatedFileSystem(directoryFactory, wrapper, rebase, manager, pathChangeHandler, new ConcurrentHashMap<>());
        child.registerRootDirectory(watchedDirectory);

        file = root_dir_path.resolve(NEW_FILE_NAME);
        child.start();
    }

    @After
    public void tearDown() throws IOException {
        child.close();
    }

    private void writeContent(final Path pPath) throws Exception {
        try (final BufferedWriter writer = newBufferedWriter(pPath, CREATE)) {
            writer.write(randomUUID().toString());

            // HFS+ has timestamps in second(!) intervals
            sleep(5000);
        }
    }

    // TODO: Use pNew parameter and check test on Linux and macOS
    private void changeContent(final Path pPath, final boolean pNew) throws Exception {
        writeContent(pPath);
        verify(pathChangeHandler, timeout(15000)).pathModified(same(dispatcher), notNull(), eq(pPath), anyBoolean());
        reset(pathChangeHandler);
    }

    @Test
    public void verifyThreadNotKillWhenRuntimeExceptionOccurs() throws Exception {
        doThrow(RuntimeException.class).when(pathChangeHandler).pathModified(same(dispatcher), notNull(), eq(file), eq(false));
        writeContent(file);
        verify(pathChangeHandler, timeout(15000)).pathModified(same(dispatcher), notNull(), eq(file), eq(true));
        assertNull(threadKiller);
    }

    @Test
    public void entryCreate() throws Exception {
        changeContent(file, true);
    }

    @Test
    public void entryModify() throws Exception {
        changeContent(testfile_txt_path, false);
    }

    @Test
    public void entryDelete() throws Exception {
        changeContent(file, true);
        delete(file);
        verify(pathChangeHandler, timeout(15000)).pathDiscarded(dispatcher, file);
    }
}
