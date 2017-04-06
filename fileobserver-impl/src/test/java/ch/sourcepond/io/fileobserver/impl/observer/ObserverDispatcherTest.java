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
import org.mockito.InOrder;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.concurrent.ExecutorService;

import static java.lang.Thread.sleep;
import static java.util.Arrays.asList;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 */
public class ObserverDispatcherTest {
    private static final Object PARENT_DIR_KEY = new Object();
    private static final Object DIR_KEY = new Object();
    private final ExecutorService dispatcherExecutor = newSingleThreadExecutor();
    private final ExecutorService observerExecutor = newSingleThreadExecutor();
    private final ObserverDispatcher dispatcher = new ObserverDispatcher();
    private final FileKey parentKey = mock(FileKey.class);
    private final Collection<FileKey> parentKeys = asList(parentKey);
    private final FileKey fileKey = mock(FileKey.class);
    private final Path file = mock(Path.class);
    private final FileObserver observer = mock(FileObserver.class);
    private final KeyDeliveryHook hook = mock(KeyDeliveryHook.class);

    @Before
    public void setup() {
        when(parentKey.directoryKey()).thenReturn(PARENT_DIR_KEY);
        when(fileKey.directoryKey()).thenReturn(DIR_KEY);
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
        verifyZeroInteractions(observer);
    }

    private void verifyHookObserverFlow() throws IOException {
        final InOrder order = inOrder(hook, observer);
        order.verify(hook, timeout(1000)).beforeModify(fileKey);
        order.verify(observer, timeout(1000)).supplement(fileKey, parentKey);
        order.verify(observer, timeout(1000)).modified(fileKey, file);
        order.verify(hook, timeout(1000)).afterModify(fileKey);
        order.verifyNoMoreInteractions();
    }

    @Test
    public void modifiedWithKeyCollection() throws IOException {
        dispatcher.modified(asList(fileKey), file, parentKeys);
        verifyHookObserverFlow();
    }

    @Test
    public void modified() throws IOException {
        dispatcher.modified(fileKey, file, parentKeys);
        verifyHookObserverFlow();
    }

    @Test
    public void modifiedCurrentlyNoHookAvailable() throws Exception {
        dispatcher.removeHook(hook);
        dispatcher.modified(fileKey, file, parentKeys);
        final InOrder order = inOrder(hook, observer);
        order.verify(observer, timeout(1000)).supplement(fileKey, parentKey);
        order.verify(observer, timeout(1000)).modified(fileKey, file);
        order.verifyNoMoreInteractions();
    }

    @Test
    public void modifiedBeforeModifiedFailed() throws IOException {
        doThrow(RuntimeException.class).when(hook).beforeModify(fileKey);
        dispatcher.modified(fileKey, file, parentKeys);
        verifyHookObserverFlow();
    }

    @Test
    public void modifiedObserverFailed() throws IOException {
        doThrow(IOException.class).when(observer).modified(fileKey, file);
        dispatcher.modified(fileKey, file, parentKeys);
        verifyHookObserverFlow();
    }

    @Test
    public void modifiedThreadInterrupted() throws Exception {
        doAnswer(inv -> {
            sleep(1000);
            return null;
        }).when(hook).beforeModify(fileKey);
        doThrow(RuntimeException.class).when(hook).beforeModify(fileKey);
        sleep(200);
        dispatcher.modified(fileKey, file, parentKeys);
        assertTrue(dispatcherExecutor.shutdownNow().isEmpty());
        verifyZeroInteractions(observer);
    }


    @Test
    public void discardCurrentlyNoObserversAvailable() {
        dispatcher.removeObserver(observer);
        dispatcher.discard(fileKey);
        verifyZeroInteractions(observer);
    }

    @Test
    public void discardCurrentlyNoHookAvailable() {
        dispatcher.removeHook(hook);
        dispatcher.discard(fileKey);
        final InOrder order = inOrder(hook, observer);
        order.verify(observer, timeout(1000)).discard(fileKey);
        order.verifyNoMoreInteractions();
    }

    @Test
    public void discard() {
        dispatcher.discard(fileKey);
        final InOrder order = inOrder(hook, observer);
        order.verify(hook, timeout(1000)).beforeDiscard(fileKey);
        order.verify(observer, timeout(1000)).discard(fileKey);
        order.verify(hook, timeout(1000)).afterDiscard(fileKey);
        order.verifyNoMoreInteractions();
    }

    @Test
    public void hasObservers() {
        assertTrue(dispatcher.hasObservers());
        dispatcher.removeObserver(observer);
        assertFalse(dispatcher.hasObservers());
        dispatcher.addObserver(observer);
        assertTrue(dispatcher.hasObservers());
    }
}
