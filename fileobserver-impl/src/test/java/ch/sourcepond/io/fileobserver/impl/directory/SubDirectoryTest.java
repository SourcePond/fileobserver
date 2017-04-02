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

import ch.sourcepond.io.fileobserver.spi.WatchedDirectory;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Collection;

import static org.junit.Assert.*;

/**
 *
 */
public class SubDirectoryTest extends DirectoryTest {
    private Directory root;
    private Directory subdir;

    @Before
    public void setup() throws IOException {
        root = factory.newRoot(wrapper.register(root_dir_path));
        subdir = factory.newBranch(root, wrapper.register(subdir_1_path));
    }

    @Test
    public void hasKeys() {
        assertFalse(subdir.hasKeys());
        subdir.addWatchedDirectory(watchedSubDir1);
        assertTrue(subdir.hasKeys());
        subdir.removeWatchedDirectory(watchedSubDir1);
        assertFalse(subdir.hasKeys());
    }

    @Test
    public void toSubDirToRootDirecory() {
        subdir.addWatchedDirectory(watchedSubDir1);
        assertFalse(subdir.isRoot());
        final Directory converted = subdir.toRootDirectory();
        assertNotSame(subdir, converted);
        assertTrue(converted.isRoot());
        assertSame(factory, converted.getFactory());
        assertSame(subdir.getWatchKey(), converted.getWatchKey());
        final Collection<WatchedDirectory> keys = converted.getWatchedDirectories();
        assertEquals(1, keys.size());
        assertTrue(keys.contains(watchedSubDir1));
    }

    @Test
    public void rebase() throws IOException {
        final Directory newRoot = factory.newRoot(wrapper.register(root_dir_path));
        assertSame(subdir, subdir.rebase(newRoot));
        assertSame(newRoot, ((SubDirectory)subdir).getParent());
    }

    @Test
    public void getParent() {
        assertSame(root, ((SubDirectory)subdir).getParent());
    }
}
