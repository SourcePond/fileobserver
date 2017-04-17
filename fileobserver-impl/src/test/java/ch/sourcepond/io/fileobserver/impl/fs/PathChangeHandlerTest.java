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
import ch.sourcepond.io.fileobserver.impl.observer.EventDispatcher;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 */
public class PathChangeHandlerTest {
    private final DedicatedFileSystem dfs = mock(DedicatedFileSystem.class);
    private final VirtualRoot virtualRoot = mock(VirtualRoot.class);
    private final EventDispatcher dispatcher = mock(EventDispatcher.class);
    private final DirectoryRegistrationWalker walker = mock(DirectoryRegistrationWalker.class);
    private final BasicFileAttributes attrs = mock(BasicFileAttributes.class);
    private final Directory directory = mock(Directory.class);
    private final Directory subDirectory = mock(Directory.class);
    private final Path parent = mock(Path.class);
    private final Path path = mock(Path.class);
    private final ConcurrentMap<Path, Directory> dirs = new ConcurrentHashMap<>();
    private final PathChangeHandler handler = new PathChangeHandler(virtualRoot, walker, dirs);

    @Before
    public void setup() {
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
        handler.pathModified(dispatcher, attrs, path);
        verify(walker).directoryCreated(dispatcher, path);
    }

    @Test
    public void fileModifed() {
        dirs.put(parent, directory);
        handler.pathModified(dispatcher, attrs, path);
        verify(directory).informIfChanged(dispatcher, path);
    }

    @Test(expected = NullPointerException.class)
    public void fileModifedNoParentRegistered() {
        handler.pathModified(dispatcher, attrs, path);
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
        order.verify(directory).cancelKey();
        order.verify(subDirectory).cancelKey();
        order.verify(directory).informDiscard(dispatcher, parent);

        assertFalse(dirs.containsKey(parent));
        assertFalse(dirs.containsKey(path));
        assertEquals(shouldNotBeRemovedDirectory, dirs.get(shouldNotBeRemovedPath));
    }
}
