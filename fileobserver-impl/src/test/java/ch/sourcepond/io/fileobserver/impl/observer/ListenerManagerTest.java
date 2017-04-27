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
    private final ExecutorService listenerExecutor = newSingleThreadExecutor();
    private final DefaultDispatchRestrictionFactory restrictionFactory = mock(DefaultDispatchRestrictionFactory.class);
    private final DefaultDispatchRestriction restriction = mock(DefaultDispatchRestriction.class);
    private final DispatchEventFactory dispatchEventFactory = mock(DispatchEventFactory.class);
    private final DispatchKey parentKey = mock(DispatchKey.class);
    private final Collection<DispatchKey> parentKeys = asList(parentKey);
    private final DispatchEvent dispatchEvent = mock(DispatchEvent.class);
    private final DispatchKey dispatchKey = mock(DispatchKey.class);
    private final FileSystem fs = mock(FileSystem.class);
    private final Path file = mock(Path.class);
    private final PathChangeListener listener = mock(PathChangeListener.class);
    private final KeyDeliveryHook hook = mock(KeyDeliveryHook.class);
    private volatile DispatchEvent realEvent;
    private ListenerManager manager = new ListenerManager(restrictionFactory, dispatchEventFactory);

    @Before
    public void setup() {
        when(dispatchEventFactory.create(listener, dispatchKey, file, parentKeys, manager)).thenReturn(dispatchEvent);
        when(file.getFileSystem()).thenReturn(fs);
        when(dispatchEvent.getKey()).thenReturn(dispatchKey);
        when(dispatchKey.getRelativePath()).thenReturn(file);
        when(restrictionFactory.createRestriction(fs)).thenReturn(restriction);
        when(restriction.isAccepted(dispatchKey)).thenReturn(true);
        when(parentKey.getDirectoryKey()).thenReturn(PARENT_DIR_KEY);
        when(dispatchKey.getDirectoryKey()).thenReturn(DIR_KEY);
        setupManager();
    }

    private void setupManager() {
        manager.setDispatcherExecutor(dispatcherExecutor);
        manager.setListenerExecutor(listenerExecutor);
        manager.addListener(listener);
        manager.addHook(hook);
    }

    @After
    public void tearDown() {
        listenerExecutor.shutdown();
    }

    @Test
    public void modifiedCurrentlyNoObserversAvailable() {
        manager.removeObserver(listener);
        manager.modified(manager.getListeners(), dispatchKey, file, parentKeys);
        verifyZeroInteractions(listener);
    }

    private void verifyHookObserverFlow() throws IOException {
        final InOrder order = inOrder(listener, restriction, hook);
        order.verify(listener).restrict(restriction);
        order.verify(restriction).isAccepted(dispatchKey);
        order.verify(hook, timeout(1000)).beforeModify(dispatchKey, file);
        order.verify(listener, timeout(1000)).supplement(dispatchKey, parentKey);
        order.verify(listener, timeout(1000)).modified(dispatchEvent);
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
        final InOrder order = inOrder(hook, listener);
        order.verify(listener, timeout(1000)).supplement(dispatchKey, parentKey);
        order.verify(listener, timeout(1000)).modified(dispatchEvent);
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
        doThrow(IOException.class).when(listener).modified(dispatchEvent);
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
        verify(listener).restrict(restriction);
        verifyNoMoreInteractions(listener);
    }

    @Test(timeout = 10000)
    public void clientWantsToReplayEvent() throws Exception {
        manager = new ListenerManager();
        doCallRealMethod().when(listener).restrict(notNull());
        doAnswer(inv -> {
            final DispatchEvent event = inv.getArgument(0);
            if (realEvent == null) {
                realEvent = event;
            }
            if (4 > event.getNumReplays()) {
                event.replay();
            }
            return null;
        }).when(listener).modified(any());
        setupManager();
        manager.modified(manager.getListeners(), dispatchKey, file, parentKeys);

        sleep(5000);
        verify(hook, times(5)).beforeModify(dispatchKey, file);
        verify(listener, times(5)).modified(realEvent);
        verify(hook, times(5)).afterModify(dispatchKey, file);
        assertEquals(4, realEvent.getNumReplays());
    }

    @Test
    public void discardCurrentlyNoObserversAvailable() {
        manager.removeObserver(listener);
        manager.discard(manager.getListeners(), dispatchKey);
        verifyZeroInteractions(listener);
    }

    @Test
    public void discardCurrentlyNoHookAvailable() {
        manager.removeHook(hook);
        manager.discard(manager.getListeners(), dispatchKey);
        final InOrder order = inOrder(hook, listener);
        order.verify(listener, timeout(1000)).discard(dispatchKey);
        order.verifyNoMoreInteractions();
    }

    @Test
    public void discard() {
        manager.discard(manager.getListeners(), dispatchKey);
        final InOrder order = inOrder(hook, listener);
        order.verify(hook, timeout(1000)).beforeDiscard(dispatchKey);
        order.verify(listener, timeout(1000)).discard(dispatchKey);
        order.verify(hook, timeout(1000)).afterDiscard(dispatchKey);
        order.verifyNoMoreInteractions();
    }
}
