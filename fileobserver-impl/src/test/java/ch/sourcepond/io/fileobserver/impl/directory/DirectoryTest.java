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

import ch.sourcepond.io.checksum.api.ResourcesFactory;
import ch.sourcepond.io.fileobserver.impl.ExecutorServices;
import ch.sourcepond.io.fileobserver.impl.filekey.DefaultFileKeyFactory;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.WatchKey;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 */
public class DirectoryTest {
    private static final Object ROOT_DIR_WATCH_KEY = new Object();
    private static final Object SUB_DIR_WATCH_KEY = new Object();
    private final ResourcesFactory resourcesFactory = mock(ResourcesFactory.class);
    private final DefaultFileKeyFactory fileKeyFactory = mock(DefaultFileKeyFactory.class);
    private final ExecutorServices executors = mock(ExecutorServices.class);
    private final DirectoryFactory factory = new DirectoryFactory(resourcesFactory, fileKeyFactory, executors);
    private final Path rootDirPath = mock(Path.class);
    private final Path subDirPath = mock(Path.class);
    private final WatchKey rootDirWatchKey = mock(WatchKey.class);
    private final WatchKey subDirWatchKey = mock(WatchKey.class);
    private final RootDirectory rootDir = new RootDirectory(factory);
    private final SubDirectory subDir = new SubDirectory(rootDir, subDirWatchKey);

    @Before
    public void setup() {
        when(rootDirWatchKey.watchable()).thenReturn(rootDirPath);
        when(subDirWatchKey.watchable()).thenReturn(subDirPath);
    }

    @Test
    public void createKeys() {
        rootDir.addDirectoryKey(ROOT_DIR_WATCH_KEY);

    }
}
