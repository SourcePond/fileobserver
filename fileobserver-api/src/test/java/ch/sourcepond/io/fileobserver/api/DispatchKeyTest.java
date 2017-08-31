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
package ch.sourcepond.io.fileobserver.api;

import org.junit.Before;
import org.junit.Test;

import java.nio.file.Path;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 */
public class DispatchKeyTest {
    private static final String ANY_NAME = "anyName";
    private static final String DIRECTORY_KEY = "directoryKey1";
    private final Path path = mock(Path.class, withSettings().name("root"));
    private final Path fileName = mock(Path.class, withSettings().name(ANY_NAME));
    private final DispatchKey key = mock(DispatchKey.class);

    private void setup(final DispatchKey pKey, final String pDirectoryKey, final Path pPath) {
        when(pKey.getRelativePath()).thenReturn(pPath);
        when(pKey.getDirectoryKey()).thenReturn(pDirectoryKey);
    }

    @Before
    public void setup() {
        when(path.getFileName()).thenReturn(fileName);
        setup(key, DIRECTORY_KEY, path);
    }

    @Test
    public void getFileName() {
        doCallRealMethod().when(key).getFileName();
        assertEquals(ANY_NAME, key.getFileName());
    }
}
