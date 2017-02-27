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
package ch.sourcepond.io.fileobserver.spi;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.spi.FileSystemProvider;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 */
public class WatchedDirectoryTest {

    private enum TestKey {
        TEST_KEY
    }

    private final Path path = mock(Path.class);
    private final Path newPath = mock(Path.class);
    private final BasicFileAttributes attrs = mock(BasicFileAttributes.class);
    private final BasicFileAttributes newAttrs = mock(BasicFileAttributes.class);
    private final FileSystem fs = mock(FileSystem.class);
    private final FileSystemProvider provider = mock(FileSystemProvider.class);
    private final RelocationObserver observer = mock(RelocationObserver.class);
    private WatchedDirectory dir;

    @Before
    public void setup() throws IOException {
        when(path.getFileSystem()).thenReturn(fs);
        when(newPath.getFileSystem()).thenReturn(fs);
        when(provider.readAttributes(path, BasicFileAttributes.class)).thenReturn(attrs);
        when(provider.readAttributes(newPath, BasicFileAttributes.class)).thenReturn(newAttrs);
        when(fs.provider()).thenReturn(provider);
        when(attrs.isDirectory()).thenReturn(true);
        when(newAttrs.isDirectory()).thenReturn(true);
        dir = WatchedDirectory.create(TestKey.TEST_KEY, path);
        dir.addObserver(observer);
    }

    @Test(expected = NullPointerException.class)
    public void createKeyIsNull() {
        WatchedDirectory.create(null, path);
    }

    @Test(expected = NullPointerException.class)
    public void createDirectoryIsNull() {
        WatchedDirectory.create(TestKey.TEST_KEY, null);
    }

    @Test
    public void create() {
        assertSame(TestKey.TEST_KEY, dir.getKey());
        assertSame(path, dir.getDirectory());
    }

    @Test(expected = NullPointerException.class)
    public void addNullObserver() {
        dir.addObserver(null);
    }

    @Test
    public void relocate() {
        dir.relocate(newPath);
        assertSame(newPath, dir.getDirectory());
        verify(observer).destinationChanged(dir, path);
    }

    @Test(expected = NullPointerException.class)
    public void relocateDirectoryIsNull() {
        dir.relocate(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void relocatePathIsNotADirectory() {
        when(newAttrs.isDirectory()).thenReturn(false);
        dir.relocate(newPath);
    }
}
