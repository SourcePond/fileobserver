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

import ch.sourcepond.io.fileobserver.impl.VirtualRoot;
import ch.sourcepond.io.fileobserver.impl.directory.Directory;
import ch.sourcepond.io.fileobserver.impl.listener.EventDispatcher;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.spi.FileSystemProvider;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 *
 */
public class PathChangeHandlerTest {
    private final DedicatedFileSystem dfs = mock(DedicatedFileSystem.class);
    private final VirtualRoot virtualRoot = mock(VirtualRoot.class);
    private final EventDispatcher dispatcher = mock(EventDispatcher.class);
    private final DirectoryRegistrationWalker walker = mock(DirectoryRegistrationWalker.class);
    private final FileSystem fs = mock(FileSystem.class);
    private final FileSystemProvider provider = mock(FileSystemProvider.class);
    private final BasicFileAttributes attrs = mock(BasicFileAttributes.class);
    private final Directory directory = mock(Directory.class);
    private final Directory subDirectory = mock(Directory.class);
    private final Path parent = mock(Path.class);
    private final Path path = mock(Path.class);
    private final ConcurrentMap<Path, Directory> dirs = new ConcurrentHashMap<>();
    private final PathChangeHandler handler = new PathChangeHandler(virtualRoot, walker, dirs);

    @Before
    public void setup() throws IOException {
        when(fs.provider()).thenReturn(provider);
        when(path.getFileSystem()).thenReturn(fs);
        when(provider.readAttributes(path, BasicFileAttributes.class)).thenReturn(attrs);
        when(path.getParent()).thenReturn(parent);
    }

    @Test
    public void rootAdded() {
        final Directory dir = mock(Directory.class);
        handler.rootAdded(dispatcher, dir);
        verify(walker).rootAdded(dispatcher, dir);
    }

    @Test
    public void removeFileSystem() {
        handler.removeFileSystem(dfs);
        verify(virtualRoot).removeFileSystem(dfs);
    }

    @Test
    public void directoryModified() {
        when(attrs.isDirectory()).thenReturn(true);
        handler.pathModified(dispatcher, path, false);
        verifyZeroInteractions(walker);
    }

    @Test
    public void directoryModifiedNewlyCreated() {
        when(attrs.isDirectory()).thenReturn(true);
        handler.pathModified(dispatcher, path, true);
        verify(walker).directoryCreated(dispatcher, path);
    }

    @Test
    public void fileModifed() {
        dirs.put(parent, directory);
        when(attrs.isRegularFile()).thenReturn(true);
        handler.pathModified(dispatcher, path, false);
        verify(directory).informIfChanged(dispatcher, path, false);
    }

    @Test(expected = NullPointerException.class)
    public void fileModifedNoParentRegistered() {
        when(attrs.isRegularFile()).thenReturn(true);
        handler.pathModified(dispatcher, path, false);
    }

    @Test
    public void fileDiscarded() {
        dirs.put(parent, directory);
        handler.pathDiscarded(dispatcher, path);
        verify(directory).informDiscard(dispatcher, path);
    }

    @Test
    public void fileDiscardedNoParentRegistered() {
        // Should not cause an exception
        handler.pathDiscarded(dispatcher, path);
    }

    @Test
    public void directoryDiscarded() {
        final Path shouldNotBeRemovedPath = mock(Path.class);
        final Directory shouldNotBeRemovedDirectory = mock(Directory.class);

        dirs.put(shouldNotBeRemovedPath, shouldNotBeRemovedDirectory);
        dirs.put(parent, directory);
        dirs.put(path, subDirectory);
        when(path.startsWith(parent)).thenReturn(true);

        handler.pathDiscarded(dispatcher, parent);

        final InOrder order = inOrder(directory, subDirectory);
        order.verify(directory).cancelKeyAndDiscardResources(dispatcher);
        order.verify(subDirectory).cancelKeyAndDiscardResources(dispatcher);

        assertFalse(dirs.containsKey(parent));
        assertFalse(dirs.containsKey(path));
        assertEquals(shouldNotBeRemovedDirectory, dirs.get(shouldNotBeRemovedPath));
    }
}
