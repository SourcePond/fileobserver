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

import ch.sourcepond.io.fileobserver.impl.pending.PendingEventRegistry;
import org.junit.Test;

import java.nio.file.Path;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 *
 */
public class SignalProcessedTest {
    private final PendingEventRegistry registry = mock(PendingEventRegistry.class);
    private final Path pending = mock(Path.class);
    private final SignalProcessed processed = new SignalProcessed(registry, pending, 2);

    @Test
    public void done() {
        processed.done();
        verifyZeroInteractions(registry);
        processed.done();
        verify(registry).done(pending);
    }
}
