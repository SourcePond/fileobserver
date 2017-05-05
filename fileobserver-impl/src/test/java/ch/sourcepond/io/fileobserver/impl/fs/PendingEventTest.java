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

import static java.lang.Thread.sleep;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

/**
 *
 */
public class PendingEventTest {
    private final Path path = mock(Path.class);
    private final PendingEvent event = new PendingEvent(path, 2000L);

    @Test(timeout = 3000)
    public void getDelay() throws Exception {
        int count = 0;
        while (event.getDelay(MILLISECONDS) > 0) {
            count++;
            sleep(100);
        }
        assertTrue(count > 10);
    }

    @Test
    public void compareTo() {
        assertEquals(1, event.compareTo(new PendingEvent(path, 1000L)));
        assertEquals(0, event.compareTo(event));
        assertEquals(-1, event.compareTo(new PendingEvent(path, 3000L)));
    }

    @Test
    public void getPath() {
        assertSame(path, event.getPath());
    }
}
