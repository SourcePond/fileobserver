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
import java.nio.file.PathMatcher;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.spi.FileSystemProvider;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 */
public class WatchedDirectoryTest {
    private static final String TEST_KEY = "TEST_KEY";
    private final Path path = mock(Path.class);
    private final Path newPath = mock(Path.class);
    private final BasicFileAttributes attrs = mock(BasicFileAttributes.class);
    private final BasicFileAttributes newAttrs = mock(BasicFileAttributes.class);
    private final FileSystem fs = mock(FileSystem.class);
    private final PathMatcher matcher = mock(PathMatcher.class);
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
        when(fs.getPathMatcher("AAA.zip")).thenReturn(matcher);
        when(fs.getPathMatcher("BBB.zip")).thenReturn(matcher);
        dir = WatchedDirectory.create(TEST_KEY, path);
        dir.addObserver(observer);
    }

    @Test
    public void isBlacklisted() {
        dir.addBlacklistPattern("AAA.zip");
        dir.addBlacklistPattern("BBB.zip");
        final Path aaa = mock(Path.class, withSettings().name("AAA.zip"));
        final Path bbb = mock(Path.class, withSettings().name("BBB.zip"));
        final Path ccc = mock(Path.class, withSettings().name("CCC.zip"));
        when(matcher.matches(aaa)).thenReturn(true);
        when(matcher.matches(bbb)).thenReturn(true);
        assertTrue(dir.isBlacklisted(aaa));
        assertTrue(dir.isBlacklisted(bbb));
        assertFalse(dir.isBlacklisted(ccc));
    }

    @Test(expected = NullPointerException.class)
    public void createKeyIsNull() {
        WatchedDirectory.create(null, path);
    }

    @Test(expected = NullPointerException.class)
    public void createDirectoryIsNull() {
        WatchedDirectory.create(TEST_KEY, null);
    }

    @Test
    public void create() {
        assertSame(TEST_KEY, dir.getKey());
        assertSame(path, dir.getDirectory());
    }

    @Test(expected = NullPointerException.class)
    public void addNullObserver() {
        dir.addObserver(null);
    }

    @Test
    public void removeNullObserver() {
        // No exception should be caused to be thrown
        dir.removeObserver(null);
    }

    @Test
    public void removeObserver() throws IOException {
        dir.removeObserver(observer);
        dir.relocate(newPath);
        verify(observer, never()).destinationChanged(any(), any());
    }

    @Test
    public void relocate() throws IOException {
        dir.relocate(newPath);
        assertSame(newPath, dir.getDirectory());
        verify(observer).destinationChanged(dir, path);
    }

    @Test
    public void relocateIOExceptionOccurred() throws IOException {
        final IOException expected = new IOException();
        doThrow(expected).when(observer).destinationChanged(dir, path);
        try {
            dir.relocate(newPath);
            fail("Exception expected");
        } catch (final IOException e) {
            assertSame(expected, e.getCause().getCause());
        }
    }

    @Test(expected = NullPointerException.class)
    public void relocateDirectoryIsNull() throws IOException {
        dir.relocate(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void relocatePathIsNotADirectory() throws IOException {
        when(newAttrs.isDirectory()).thenReturn(false);
        dir.relocate(newPath);
    }

    @Test
    public void verifyToString() {
        assertTrue(dir.toString().contains(TEST_KEY));
        assertTrue(dir.toString().contains(path.toString()));
    }
}
