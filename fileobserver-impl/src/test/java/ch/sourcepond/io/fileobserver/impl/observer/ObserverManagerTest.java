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
import ch.sourcepond.io.fileobserver.impl.restriction.DefaultDispatchRestriction;
import ch.sourcepond.io.fileobserver.impl.restriction.DefaultDispatchRestrictionFactory;
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
public class ObserverManagerTest {
    private static final Object PARENT_DIR_KEY = new Object();
    private static final Object DIR_KEY = new Object();
    private final ExecutorService dispatcherExecutor = newSingleThreadExecutor();
    private final ExecutorService observerExecutor = newSingleThreadExecutor();
    private final DefaultDispatchRestrictionFactory restrictionFactory = mock(DefaultDispatchRestrictionFactory.class);
    private final DefaultDispatchRestriction restriction = mock(DefaultDispatchRestriction.class);
    private final ObserverManager manager = new ObserverManager(restrictionFactory);
    private final FileKey parentKey = mock(FileKey.class);
    private final Collection<FileKey> parentKeys = asList(parentKey);
    private final FileKey fileKey = mock(FileKey.class);
    private final Path file = mock(Path.class);
    private final FileObserver observer = mock(FileObserver.class);
    private final KeyDeliveryHook hook = mock(KeyDeliveryHook.class);

    @Before
    public void setup() {
        when(restrictionFactory.createRestriction()).thenReturn(restriction);
        when(restriction.isAccepted(fileKey)).thenReturn(true);
        when(parentKey.getDirectoryKey()).thenReturn(PARENT_DIR_KEY);
        when(fileKey.getDirectoryKey()).thenReturn(DIR_KEY);
        manager.setDispatcherExecutor(dispatcherExecutor);
        manager.setObserverExecutor(observerExecutor);
        manager.addObserver(observer);
        manager.addHook(hook);
    }

    @After
    public void tearDown() {
        observerExecutor.shutdown();
    }

    @Test
    public void modifiedCurrentlyNoObserversAvailable() {
        manager.removeObserver(observer);
        manager.modified(manager.getObservers(), fileKey, file, parentKeys);
        verify(observer).setup(restriction);
        verifyNoMoreInteractions(observer);
    }

    private void verifyHookObserverFlow() throws IOException {
        final InOrder order = inOrder(hook, observer);
        order.verify(hook, timeout(1000)).beforeModify(fileKey, file);
        order.verify(observer, timeout(1000)).supplement(fileKey, parentKey);
        order.verify(observer, timeout(1000)).modified(fileKey, file);
        order.verify(hook, timeout(1000)).afterModify(fileKey, file);
        order.verifyNoMoreInteractions();
    }

    @Test
    public void modifiedWithKeyCollection() throws IOException {
        manager.modified(manager.getObservers(), asList(fileKey), file, parentKeys);
        verifyHookObserverFlow();
    }

    @Test
    public void modified() throws IOException {
        manager.modified(manager.getObservers(), fileKey, file, parentKeys);
        verifyHookObserverFlow();
    }

    @Test
    public void modifiedNotAccepted() throws IOException {
        when(restriction.isAccepted(fileKey)).thenReturn(false);
        manager.modified(manager.getObservers(), fileKey, file, parentKeys);
        verifyZeroInteractions(hook);
    }

    @Test
    public void modifiedCurrentlyNoHookAvailable() throws Exception {
        manager.removeHook(hook);
        manager.modified(manager.getObservers(), fileKey, file, parentKeys);
        final InOrder order = inOrder(hook, observer);
        order.verify(observer, timeout(1000)).supplement(fileKey, parentKey);
        order.verify(observer, timeout(1000)).modified(fileKey, file);
        order.verifyNoMoreInteractions();
    }

    @Test
    public void modifiedBeforeModifiedFailed() throws IOException {
        doThrow(RuntimeException.class).when(hook).beforeModify(fileKey, file);
        manager.modified(manager.getObservers(), fileKey, file, parentKeys);
        verifyHookObserverFlow();
    }

    @Test
    public void modifiedObserverFailed() throws IOException {
        doThrow(IOException.class).when(observer).modified(fileKey, file);
        manager.modified(manager.getObservers(), fileKey, file, parentKeys);
        verifyHookObserverFlow();
    }

    @Test
    public void modifiedThreadInterrupted() throws Exception {
        doAnswer(inv -> {
            sleep(1000);
            return null;
        }).when(hook).beforeModify(fileKey, file);
        doThrow(RuntimeException.class).when(hook).beforeModify(fileKey, file);
        sleep(200);
        manager.modified(manager.getObservers(), fileKey, file, parentKeys);
        assertTrue(dispatcherExecutor.shutdownNow().isEmpty());
        verify(observer).setup(restriction);
        verifyNoMoreInteractions(observer);
    }


    @Test
    public void discardCurrentlyNoObserversAvailable() {
        manager.removeObserver(observer);
        manager.discard(manager.getObservers(), fileKey);
        verify(observer).setup(restriction);
        verifyNoMoreInteractions(observer);
    }

    @Test
    public void discardCurrentlyNoHookAvailable() {
        manager.removeHook(hook);
        manager.discard(manager.getObservers(), fileKey);
        final InOrder order = inOrder(hook, observer);
        order.verify(observer, timeout(1000)).discard(fileKey);
        order.verifyNoMoreInteractions();
    }

    @Test
    public void discard() {
        manager.discard(manager.getObservers(), fileKey);
        final InOrder order = inOrder(hook, observer);
        order.verify(hook, timeout(1000)).beforeDiscard(fileKey);
        order.verify(observer, timeout(1000)).discard(fileKey);
        order.verify(hook, timeout(1000)).afterDiscard(fileKey);
        order.verifyNoMoreInteractions();
    }
}
