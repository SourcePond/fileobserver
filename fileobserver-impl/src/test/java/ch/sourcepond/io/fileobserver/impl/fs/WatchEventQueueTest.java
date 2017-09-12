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

import java.nio.file.WatchEvent;
import java.util.LinkedList;
import java.util.List;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class WatchEventQueueTest {
    private final WatchEventQueue queue = new WatchEventQueue();
    private final List<WatchEvent.Kind<?>> events = new LinkedList<>();

    @Test
    public void verifyEmptyAfterProcessing() {
        queue.push(ENTRY_CREATE);
        queue.push(ENTRY_MODIFY);
        queue.push(ENTRY_DELETE);
        queue.processQueue(k -> {});
        queue.processQueue(k -> events.add(k));
        assertTrue(events.isEmpty());
    }

    @Test
    public void ignoreIfPreviousIsEventKindIsEqual() {
        queue.push(ENTRY_MODIFY);
        queue.push(ENTRY_MODIFY);
        queue.push(ENTRY_MODIFY);
        queue.push(ENTRY_DELETE);
        queue.processQueue(k -> events.add(k));

        assertEquals(2, events.size());
        assertSame(ENTRY_MODIFY, events.get(0));
        assertSame(ENTRY_DELETE, events.get(1));
    }

    @Test
    public void replaceReCreateWithModify() {
        queue.push(ENTRY_CREATE);
        queue.push(ENTRY_DELETE);
        queue.push(ENTRY_CREATE);
        queue.push(ENTRY_DELETE);
        queue.push(ENTRY_CREATE);
        queue.push(ENTRY_DELETE);

        queue.processQueue(k -> events.add(k));
        assertEquals(4, events.size());
        assertSame(ENTRY_CREATE, events.get(0));
        assertSame(ENTRY_MODIFY, events.get(1));
        assertSame(ENTRY_MODIFY, events.get(2));
        assertSame(ENTRY_DELETE, events.get(3));
    }
}
