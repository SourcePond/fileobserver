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

import ch.sourcepond.io.fileobserver.api.DispatchEvent;
import ch.sourcepond.io.fileobserver.api.DispatchKey;
import ch.sourcepond.io.fileobserver.api.PathChangeListener;
import ch.sourcepond.io.fileobserver.api.KeyDeliveryHook;
import ch.sourcepond.io.fileobserver.impl.restriction.DefaultDispatchRestriction;
import ch.sourcepond.io.fileobserver.impl.restriction.DefaultDispatchRestrictionFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

import java.io.IOException;
import java.nio.file.FileSystem;
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
public class ListenerManagerTest {
    private static final Object PARENT_DIR_KEY = new Object();
    private static final Object DIR_KEY = new Object();
    private final ExecutorService dispatcherExecutor = newSingleThreadExecutor();
    private final ExecutorService observerExecutor = newSingleThreadExecutor();
    private final DefaultDispatchRestrictionFactory restrictionFactory = mock(DefaultDispatchRestrictionFactory.class);
    private final DefaultDispatchRestriction restriction = mock(DefaultDispatchRestriction.class);
    private final DispatchEventFactory dispatchEventFactory = mock(DispatchEventFactory.class);
    private final ListenerManager manager = new ListenerManager(restrictionFactory, dispatchEventFactory);
    private final DispatchKey parentKey = mock(DispatchKey.class);
    private final Collection<DispatchKey> parentKeys = asList(parentKey);
    private final DispatchEvent dispatchEvent = mock(DispatchEvent.class);
    private final DispatchKey dispatchKey = mock(DispatchKey.class);
    private final FileSystem fs = mock(FileSystem.class);
    private final Path file = mock(Path.class);
    private final PathChangeListener observer = mock(PathChangeListener.class);
    private final KeyDeliveryHook hook = mock(KeyDeliveryHook.class);

    @Before
    public void setup() {
        when(dispatchEventFactory.create(observer, dispatchKey, file, parentKeys, manager)).thenReturn(dispatchEvent);
        when(file.getFileSystem()).thenReturn(fs);
        when(dispatchEvent.getKey()).thenReturn(dispatchKey);
        when(dispatchKey.getRelativePath()).thenReturn(file);
        when(restrictionFactory.createRestriction(fs)).thenReturn(restriction);
        when(restriction.isAccepted(dispatchKey)).thenReturn(true);
        when(parentKey.getDirectoryKey()).thenReturn(PARENT_DIR_KEY);
        when(dispatchKey.getDirectoryKey()).thenReturn(DIR_KEY);
        manager.setDispatcherExecutor(dispatcherExecutor);
        manager.setListenerExecutor(observerExecutor);
        manager.addListener(observer);
        manager.addHook(hook);
    }

    @After
    public void tearDown() {
        observerExecutor.shutdown();
    }

    @Test
    public void modifiedCurrentlyNoObserversAvailable() {
        manager.removeObserver(observer);
        manager.modified(manager.getListeners(), dispatchKey, file, parentKeys);
        verifyZeroInteractions(observer);
    }

    private void verifyHookObserverFlow() throws IOException {
        final InOrder order = inOrder(observer, restriction, hook);
        order.verify(observer).restrict(restriction);
        order.verify(restriction).isAccepted(dispatchKey);
        order.verify(hook, timeout(1000)).beforeModify(dispatchKey, file);
        order.verify(observer, timeout(1000)).supplement(dispatchKey, parentKey);
        order.verify(observer, timeout(1000)).modified(dispatchEvent);
        order.verify(hook, timeout(1000)).afterModify(dispatchKey, file);
        order.verifyNoMoreInteractions();
    }

    @Test
    public void removeFileSystem() throws IOException {
        manager.modified(manager.getListeners(), dispatchKey, file, parentKeys);
        manager.removeFileSystem(fs);
        manager.modified(manager.getListeners(), dispatchKey, file, parentKeys);
        verify(restrictionFactory, times(2)).createRestriction(fs);
    }

    @Test
    public void modifiedWithKeyCollection() throws IOException {
        manager.modified(manager.getListeners(), asList(dispatchKey), file, parentKeys);
        verifyHookObserverFlow();
    }

    @Test
    public void modified() throws IOException {
        manager.modified(manager.getListeners(), dispatchKey, file, parentKeys);
        verifyHookObserverFlow();
    }

    @Test
    public void modifiedNotAccepted() throws IOException {
        when(restriction.isAccepted(dispatchKey)).thenReturn(false);
        manager.modified(manager.getListeners(), dispatchKey, file, parentKeys);
        verifyZeroInteractions(hook);
    }

    @Test
    public void modifiedCurrentlyNoHookAvailable() throws Exception {
        manager.removeHook(hook);
        manager.modified(manager.getListeners(), dispatchKey, file, parentKeys);
        final InOrder order = inOrder(hook, observer);
        order.verify(observer, timeout(1000)).supplement(dispatchKey, parentKey);
        order.verify(observer, timeout(1000)).modified(dispatchEvent);
        order.verifyNoMoreInteractions();
    }

    @Test
    public void modifiedBeforeModifiedFailed() throws IOException {
        doThrow(RuntimeException.class).when(hook).beforeModify(dispatchKey, file);
        manager.modified(manager.getListeners(), dispatchKey, file, parentKeys);
        verifyHookObserverFlow();
    }

    @Test
    public void modifiedObserverFailed() throws IOException {
        doThrow(IOException.class).when(observer).modified(dispatchEvent);
        manager.modified(manager.getListeners(), dispatchKey, file, parentKeys);
        verifyHookObserverFlow();
    }

    @Test
    public void modifiedThreadInterrupted() throws Exception {
        doAnswer(inv -> {
            sleep(1000);
            return null;
        }).when(hook).beforeModify(dispatchKey, file);
        doThrow(RuntimeException.class).when(hook).beforeModify(dispatchKey, file);
        sleep(200);
        manager.modified(manager.getListeners(), dispatchKey, file, parentKeys);
        assertTrue(dispatcherExecutor.shutdownNow().isEmpty());
        verify(observer).restrict(restriction);
        verifyNoMoreInteractions(observer);
    }

    @Test
    public void discardCurrentlyNoObserversAvailable() {
        manager.removeObserver(observer);
        manager.discard(manager.getListeners(), dispatchKey);
        verifyZeroInteractions(observer);
    }

    @Test
    public void discardCurrentlyNoHookAvailable() {
        manager.removeHook(hook);
        manager.discard(manager.getListeners(), dispatchKey);
        final InOrder order = inOrder(hook, observer);
        order.verify(observer, timeout(1000)).discard(dispatchKey);
        order.verifyNoMoreInteractions();
    }

    @Test
    public void discard() {
        manager.discard(manager.getListeners(), dispatchKey);
        final InOrder order = inOrder(hook, observer);
        order.verify(hook, timeout(1000)).beforeDiscard(dispatchKey);
        order.verify(observer, timeout(1000)).discard(dispatchKey);
        order.verify(hook, timeout(1000)).afterDiscard(dispatchKey);
        order.verifyNoMoreInteractions();
    }
}
