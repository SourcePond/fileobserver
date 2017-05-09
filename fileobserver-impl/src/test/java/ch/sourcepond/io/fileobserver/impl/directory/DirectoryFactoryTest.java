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

import ch.sourcepond.io.fileobserver.impl.dispatch.DefaultDispatchKeyFactory;
import ch.sourcepond.io.fileobserver.impl.pending.PendingEventRegistry;
import org.junit.Test;

import java.util.concurrent.ExecutorService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 *
 */
public class DirectoryFactoryTest {
    private final ExecutorService directoryWalkerExecutor = mock(ExecutorService.class);
    private final ExecutorService listenerExecutor = mock(ExecutorService.class);
    private final PendingEventRegistry registry = mock(PendingEventRegistry.class);
    private final DefaultDispatchKeyFactory keyFactory = mock(DefaultDispatchKeyFactory.class);
    private final DirectoryFactory factory = new DirectoryFactory(registry, keyFactory);

    @Test
    public void shutdown() {
        factory.setExecutors(directoryWalkerExecutor, listenerExecutor);
        factory.shutdown();
        verify(directoryWalkerExecutor).shutdown();
        verify(listenerExecutor).shutdown();
    }
}
