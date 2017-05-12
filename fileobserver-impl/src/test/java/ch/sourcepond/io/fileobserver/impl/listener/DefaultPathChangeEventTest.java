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
package ch.sourcepond.io.fileobserver.impl.listener;

import ch.sourcepond.io.fileobserver.api.DispatchKey;
import ch.sourcepond.io.fileobserver.api.PathChangeListener;
import org.junit.Test;

import java.nio.file.Path;
import java.util.Collection;

import static ch.sourcepond.io.fileobserver.impl.fs.DedicatedFileSystem.EMPTY_CALLBACK;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 */
public class DefaultPathChangeEventTest {
    private final PathChangeListener listener = mock(PathChangeListener.class);
    private final Path file = mock(Path.class);
    private final Collection<DispatchKey> parentKeys = mock(Collection.class);
    private final DispatchKey key = mock(DispatchKey.class);
    private final ReplayDispatcher replayDispatcher = mock(ReplayDispatcher.class);
    private final DefaultPathChangeEvent event = new DefaultPathChangeEvent(listener, key, file, parentKeys, replayDispatcher);

    @Test
    public void getKey() {
        assertSame(key, event.getKey());
    }

    @Test
    public void getFile() {
        assertSame(file, event.getFile());
    }

    @Test
    public void verifyReplay() {
        assertEquals(0, event.getNumReplays());
        event.replay();
        assertEquals(1, event.getNumReplays());
        event.replay();
        assertEquals(2, event.getNumReplays());
        verify(replayDispatcher, times(2)).replay(EMPTY_CALLBACK, listener, event, parentKeys);
    }

    @Test
    public void verifyToString() {
        assertEquals("PathChangeEvent[key: " + key + ", numReplays: 0, file: " + file + "]", event.toString());
    }
}
