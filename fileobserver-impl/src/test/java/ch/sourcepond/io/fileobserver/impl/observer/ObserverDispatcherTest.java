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
package ch.sourcepond.io.fileobserver.impl.observer;

import ch.sourcepond.io.fileobserver.api.FileKey;
import ch.sourcepond.io.fileobserver.api.FileObserver;
import ch.sourcepond.io.fileobserver.api.KeyDeliveryHook;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Path;
import java.util.Collection;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.mockito.Mockito.mock;

/**
 *
 */
public class ObserverDispatcherTest {
    private final ExecutorService dispatcherExecutor = newSingleThreadExecutor();
    private final ExecutorService observerExecutor = newSingleThreadExecutor();
    private final ObserverDispatcher dispatcher = new ObserverDispatcher();
    private final Collection<FileKey> parentKeys = mock(Collection.class);
    private final FileKey fileKey = mock(FileKey.class);
    private final Path file = mock(Path.class);
    private final FileObserver observer = mock(FileObserver.class);
    private final KeyDeliveryHook hook = mock(KeyDeliveryHook.class);

    @Before
    public void setup() {
        dispatcher.setDispatcherExecutor(dispatcherExecutor);
        dispatcher.setObserverExecutor(observerExecutor);
        dispatcher.addObserver(observer);
        dispatcher.addHook(hook);
    }

    @After
    public void tearDown() {
        observerExecutor.shutdown();
    }

    @Test
    public void modifiedCurrentlyNoObserversAvailable() {
        dispatcher.removeObserver(observer);
        dispatcher.modified(fileKey, file, parentKeys);
    }

}
