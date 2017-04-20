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
package ch.sourcepond.io.fileobserver.impl.filekey;

import ch.sourcepond.io.fileobserver.api.FileKey;
import org.junit.Test;

import java.nio.file.Path;
import java.util.Objects;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 */
public class DefaultFileKeyTest {
    private static final String DIRECTORY_KEY_1 = "directoryKey1";
    private static final String DIRECTORY_KEY_2 = "directoryKey2";
    private final Path path = mock(Path.class, withSettings().name("root"));
    private Path otherPath = mock(Path.class);
    private final FileKey<Object> key1 = new DefaultFileKeyFactory().newKey(DIRECTORY_KEY_1, path);
    private final FileKey<Object> key2 = new DefaultFileKeyFactory().newKey(DIRECTORY_KEY_1, path);
    private final FileKey<Object> key3 = new DefaultFileKeyFactory().newKey(DIRECTORY_KEY_1, otherPath);
    private FileKey<?> key4 = new DefaultFileKeyFactory().newKey(DIRECTORY_KEY_2, otherPath);

    @Test
    public void key() {
        assertSame(DIRECTORY_KEY_1, key1.getDirectoryKey());
    }

    @Test
    public void relativePath() {
        assertSame(path, key1.getRelativePath());
    }

    @Test
    public void verifyEquals() {
        assertTrue(key1.equals(key1));
        assertFalse(key1.equals(null));
        assertFalse(key1.equals(new Object()));
        assertTrue(key1.equals(key2));
        assertFalse(key1.equals(key3));
        assertFalse(key2.equals(key4));
    }

    @Test
    public void verifyToString() {
        otherPath = mock(Path.class, withSettings().name("SOME_PATH"));
        key4 = new DefaultFileKeyFactory().newKey(DIRECTORY_KEY_2, otherPath);
        assertEquals("[directoryKey2:SOME_PATH]", key4.toString());
    }

    @Test
    public void verifyHashCode() {
        assertEquals(Objects.hash(DIRECTORY_KEY_1, path), key1.hashCode());
    }
}
