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

import org.junit.Test;

import java.nio.file.FileSystem;

import static org.mockito.Mockito.*;

/**
 *
 */
public class PathChangeListenerTest {
    private final DispatchRestriction restriction = mock(DispatchRestriction.class);
    private final FileSystem fs = mock(FileSystem.class);
    private final DispatchKey key = mock(DispatchKey.class);
    private final PathChangeListener listener = mock(PathChangeListener.class);

    @Test
    public void setup() {
        doCallRealMethod().when(listener).restrict(restriction, fs);
        listener.restrict(restriction, fs);
        verify(restriction).acceptAll();
        verifyZeroInteractions(fs);
    }

    @Test
    public void supplement() {
        doCallRealMethod().when(listener).supplement(key, key);
        verifyZeroInteractions(key);
    }
}
