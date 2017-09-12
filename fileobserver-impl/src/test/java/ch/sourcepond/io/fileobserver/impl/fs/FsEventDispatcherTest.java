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

import ch.sourcepond.io.fileobserver.impl.Config;
import ch.sourcepond.io.fileobserver.impl.directory.Directory;
import ch.sourcepond.io.fileobserver.impl.listener.EventDispatcher;
import ch.sourcepond.io.fileobserver.impl.listener.ListenerManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.spi.FileSystemProvider;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.lang.Thread.sleep;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

public class FsEventDispatcherTest {
    private final long EXPECTED_DELAY = 1000L;
    private final Config config = mock(Config.class);
    private final ConcurrentMap<Path, Directory> dirs = new ConcurrentHashMap<>();
    private final DirectoryRegistrationWalker walker = mock(DirectoryRegistrationWalker.class);
    private final WatchServiceWrapper wrapper = mock(WatchServiceWrapper.class);
    private final EventDispatcher defaultDispatcher = mock(EventDispatcher.class);
    private final ListenerManager manager = mock(ListenerManager.class);
    private final WatchKey watchKey = mock(WatchKey.class);
    private final WatchEvent<Path> watchEvent = mock(WatchEvent.class);
    private final FileSystem fs = mock(FileSystem.class);
    private final FileSystemProvider provider = mock(FileSystemProvider.class);
    private final Path watchable = mock(Path.class, withSettings().name("watchable"));
    private final Path context = mock(Path.class, withSettings().name("context"));
    private final Path path = mock(Path.class, withSettings().name("path"));
    private final BasicFileAttributes pathAttrs = mock(BasicFileAttributes.class);
    private final Directory watchableDirectory = mock(Directory.class);
    private final FsEventDispatcher dispatcher = new FsEventDispatcher(dirs, walker, wrapper, manager);

    @Before
    public void setup() throws Exception {
        dirs.put(watchable, watchableDirectory);
        when(watchable.getFileSystem()).thenReturn(fs);
        when(context.getFileSystem()).thenReturn(fs);
        when(path.getFileSystem()).thenReturn(fs);
        when(path.getParent()).thenReturn(watchable);
        when(provider.readAttributes(path, BasicFileAttributes.class)).thenReturn(pathAttrs);
        when(fs.provider()).thenReturn(provider);
        when(config.eventDispatchDelayMillis()).thenReturn(EXPECTED_DELAY);
        when(wrapper.take()).thenReturn(watchKey).thenAnswer(iom -> {
            sleep(100000);
            return null;
        });
        when(watchKey.watchable()).thenReturn(watchable);
        when(watchKey.pollEvents()).thenReturn(asList(watchEvent));
        when(watchEvent.context()).thenReturn(context);
        when(watchable.resolve(context)).thenReturn(path);
        when(manager.getDefaultDispatcher()).thenReturn(defaultDispatcher);
        dispatcher.setConfig(config);
    }

    @After
    public void tearDown() {
        dispatcher.close();
    }

    @Test
    public void verifyStart() {
        dispatcher.start();
        assertTrue(dispatcher.receiverThread.isDaemon());
        assertTrue(dispatcher.receiverThread.isAlive());
    }

    @Test
    public void verifyBreakLoopWhenInterrupted() throws Exception {
        dispatcher.receiverThread.interrupt();
        sleep(1500);
        verifyZeroInteractions(watchKey);
    }

    @Test
    public void verifyIgnoreDirectoryModified() throws Exception {
        when(pathAttrs.isDirectory()).thenReturn(true);
        when(watchEvent.kind()).thenReturn(ENTRY_MODIFY);
        dispatcher.start();
        sleep(1500);
        verifyZeroInteractions(walker, watchableDirectory);
        verify(watchKey).reset();
        assertTrue(dispatcher.queues.isEmpty());
    }

    @Test
    public void verifyDirectoryCreated() throws Exception {
        when(pathAttrs.isDirectory()).thenReturn(true);
        when(watchEvent.kind()).thenReturn(ENTRY_CREATE);
        dispatcher.start();
        verify(walker, after(1500)).directoryCreated(defaultDispatcher, path);
        verifyZeroInteractions(watchableDirectory);
        verify(watchKey).reset();
        assertTrue(dispatcher.queues.isEmpty());
    }

    @Test
    public void verifyDirectoryDiscarded() throws Exception {
        when(pathAttrs.isDirectory()).thenReturn(true);
        when(watchEvent.kind()).thenReturn(ENTRY_DELETE);
        final Directory pathDirectory = mock(Directory.class);
        dirs.put(path, pathDirectory);
        final Path subPath = mock(Path.class, withSettings().name("subPath"));
        final Directory subDirectory = mock(Directory.class);
        when(subPath.startsWith(path)).thenReturn(true);
        dirs.put(subPath, subDirectory);
        dispatcher.start();

        verify(pathDirectory, after(1500)).cancelKeyAndDiscardResources(defaultDispatcher);
        verify(subDirectory, after(1500)).cancelKeyAndDiscardResources(defaultDispatcher);
        verify(watchableDirectory, never()).cancelKeyAndDiscardResources(defaultDispatcher);
        verify(watchKey).reset();

        assertEquals(1, dirs.size());
        assertSame(watchableDirectory, dirs.values().iterator().next());
        assertTrue(dispatcher.queues.isEmpty());
    }


    @Test
    public void verifyFileCreated() throws Exception {
        when(watchEvent.kind()).thenReturn(ENTRY_CREATE);
        dispatcher.start();
        verify(watchableDirectory, after(1500)).informIfChanged(defaultDispatcher, path, true);
        verifyNoMoreInteractions(watchableDirectory);
        verify(watchKey).reset();
        assertTrue(dispatcher.queues.isEmpty());
    }

    @Test
    public void verifyFileModified() throws Exception {
        when(watchEvent.kind()).thenReturn(ENTRY_MODIFY);
        dispatcher.start();
        verify(watchableDirectory, after(1500)).informIfChanged(defaultDispatcher, path, false);
        verifyNoMoreInteractions(watchableDirectory);
        verify(watchKey).reset();
        assertTrue(dispatcher.queues.isEmpty());
    }

    @Test
    public void verifyFileWithUnknownParentModified() throws Exception {
        when(path.getParent()).thenReturn(mock(Path.class));
        when(watchEvent.kind()).thenReturn(ENTRY_MODIFY);
        dispatcher.start();
        sleep(1500);
        verifyNoMoreInteractions(watchableDirectory);
        verify(watchKey).reset();
        assertTrue(dispatcher.receiverThread.isAlive());
        assertTrue(dispatcher.queues.isEmpty());
    }

    @Test
    public void verifyFileDiscarded() throws Exception {
        when(watchEvent.kind()).thenReturn(ENTRY_DELETE);
        dispatcher.start();
        verify(watchableDirectory, after(1500)).informDiscard(defaultDispatcher, path);
        verifyNoMoreInteractions(watchableDirectory);
        verify(watchKey).reset();
        assertTrue(dispatcher.queues.isEmpty());
    }

    @Test
    public void verifyFileWithUnknownParentDiscarded() throws Exception {
        when(path.getParent()).thenReturn(mock(Path.class));
        when(watchEvent.kind()).thenReturn(ENTRY_DELETE);
        dispatcher.start();
        sleep(1500);
        verifyNoMoreInteractions(watchableDirectory);
        verify(watchKey).reset();
        assertTrue(dispatcher.queues.isEmpty());
    }
}
