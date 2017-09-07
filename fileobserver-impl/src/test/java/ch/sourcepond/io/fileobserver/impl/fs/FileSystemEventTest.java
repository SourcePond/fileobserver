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

import org.junit.Test;

import java.nio.file.Path;
import java.time.Instant;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class FileSystemEventTest {
    private static final long EXPECTED_TIMEOUT = 10000L;
    private final Instant creationTime = Instant.now();
    private final Path path = mock(Path.class);
    private final FileSystemEvent event = new FileSystemEvent(creationTime, EXPECTED_TIMEOUT, ENTRY_MODIFY, path);

    @Test
    public void getKind() {
        assertEquals(ENTRY_MODIFY, event.getKind());
    }

    @Test
    public void getPath() {
        assertEquals(path, event.getPath());
    }

    @Test
    public void getDelay() {
        assertTrue(10 >= event.getDelay(SECONDS));
        assertTrue(0 <= event.getDelay(SECONDS));
    }

    @Test
    public void compareTo() {
        assertEquals(1, event.compareTo(new FileSystemEvent(creationTime, 1000, ENTRY_MODIFY, path)));
        assertEquals(-1, event.compareTo(new FileSystemEvent(creationTime, 20000, ENTRY_MODIFY, path)));
        assertEquals(0, event.compareTo(new FileSystemEvent(creationTime, EXPECTED_TIMEOUT, ENTRY_MODIFY, path)));
    }

    @Test
    public void verifyEquals() {
        assertTrue(event.equals(event));
        assertFalse(event.equals(null));
        assertFalse(event.equals(new Object()));
        assertEquals(event, new FileSystemEvent(creationTime, EXPECTED_TIMEOUT, ENTRY_MODIFY, path));
        assertEquals(event, new FileSystemEvent(creationTime, 12000, ENTRY_MODIFY, path));
        assertNotEquals(event, new FileSystemEvent(creationTime, EXPECTED_TIMEOUT, ENTRY_CREATE, path));
        assertNotEquals(event, new FileSystemEvent(creationTime, EXPECTED_TIMEOUT, ENTRY_MODIFY, mock(Path.class)));
    }

    @Test
    public void verifyHashCode() {
        assertEquals(event.hashCode(), new FileSystemEvent(creationTime, EXPECTED_TIMEOUT, ENTRY_MODIFY, path).hashCode());
        assertEquals(event.hashCode(), new FileSystemEvent(creationTime, 12000, ENTRY_MODIFY, path).hashCode());
        assertNotEquals(event.hashCode(), new FileSystemEvent(creationTime, EXPECTED_TIMEOUT, ENTRY_CREATE, path).hashCode());
        assertNotEquals(event.hashCode(), new FileSystemEvent(creationTime, EXPECTED_TIMEOUT, ENTRY_MODIFY, mock(Path.class)).hashCode());
    }
}
