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

import ch.sourcepond.io.fileobserver.impl.listener.EventDispatcher;
import ch.sourcepond.io.fileobserver.impl.listener.ListenerManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.util.concurrent.ArrayBlockingQueue;

import static java.lang.Thread.setDefaultUncaughtExceptionHandler;
import static java.lang.Thread.sleep;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;
import static java.util.Arrays.asList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@Ignore
public class DelayedPathChangeDispatcherTest {
    private static final long EXPECTED_TIMEOUT = 1000L;
    private static final long SLEEP_TIME = 500L;
    private final ArrayBlockingQueue<WatchKey> testQueue = new ArrayBlockingQueue<>(1);
    private final WatchKey watchKey = mock(WatchKey.class);
    private final WatchEvent watchEvent = mock(WatchEvent.class);
    private final WatchServiceWrapper wrapper = mock(WatchServiceWrapper.class);
    private final PathChangeHandler handler = mock(PathChangeHandler.class);
    private final EventDispatcher eventDispatcher = mock(EventDispatcher.class);
    private final ListenerManager listenerManager = mock(ListenerManager.class);
    private final Path watchable = mock(Path.class);
    private final Path context = mock(Path.class);
    private final Path file = mock(Path.class);
    private final RuntimeException exception = new RuntimeException();
    private final DelayedPathChangeDispatcher dispatcher = new DelayedPathChangeDispatcher(wrapper, handler, listenerManager);
    private final Thread.UncaughtExceptionHandler uncaughtExceptionHandler = mock(Thread.UncaughtExceptionHandler.class);

    @Before
    public void setup() throws Exception {
        when(watchEvent.kind()).thenReturn(ENTRY_MODIFY);
        when(watchEvent.context()).thenReturn(context);
        when(listenerManager.getDefaultDispatcher()).thenReturn(eventDispatcher);
        when(watchKey.pollEvents()).thenReturn(asList(watchEvent));
        when(wrapper.take()).thenAnswer(iom -> testQueue.take());
        when(watchKey.watchable()).thenReturn(watchable);
        when(watchable.resolve(context)).thenReturn(file);
        setDefaultUncaughtExceptionHandler(uncaughtExceptionHandler);
    }

    @After
    public void tearDown() throws Exception {
        setDefaultUncaughtExceptionHandler(uncaughtExceptionHandler);
        dispatcher.close();
    }

    @Test
    public void close() {
        dispatcher.close();
        verify(wrapper).close();
    }

    @Test(timeout = 3000L)
    public void overflow() throws Exception {
        when(watchEvent.kind()).thenReturn(OVERFLOW);
        testQueue.offer(watchKey);
        dispatcher.start();
        sleep(2000L);
        verifyZeroInteractions(handler);
        verify(watchKey).reset();
    }

    @Test(timeout = 300000L)
    public void pathModified() throws Exception {
        testQueue.offer(watchKey);
        dispatcher.start();
        verify(handler, timeout(300000L)).pathModified(eventDispatcher, file, false);
        verify(watchKey).reset();
    }

    @Test(timeout = 3000L)
    public void pathCreated() throws Exception {
        when(watchEvent.kind()).thenReturn(ENTRY_CREATE);
        testQueue.offer(watchKey);
        dispatcher.start();
        verify(handler, timeout(3000L)).pathModified(eventDispatcher, file, true);
        verify(watchKey).reset();
    }

    @Test(timeout = 3000L)
    public void pathDiscarded() throws Exception {
        when(watchEvent.kind()).thenReturn(ENTRY_DELETE);
        testQueue.offer(watchKey);
        dispatcher.start();
        verify(handler, timeout(3000L)).pathDiscarded(eventDispatcher, file);
        verify(watchKey).reset();
    }


    @Test(timeout = 3000L)
    public void pathModifiedExceptionOccurred() throws Exception {
        doThrow(exception).when(handler).pathModified(eventDispatcher, file, false);
        testQueue.offer(watchKey);
        dispatcher.start();
        verify(handler, timeout(3000L)).pathModified(eventDispatcher, file, false);
        sleep(SLEEP_TIME);
        verifyZeroInteractions(uncaughtExceptionHandler);
        verify(watchKey).reset();
    }

    @Test(timeout = 3000L)
    public void pathCreatedExceptionOccurred() throws Exception {
        doThrow(exception).when(handler).pathModified(eventDispatcher, file, true);
        when(watchEvent.kind()).thenReturn(ENTRY_CREATE);
        testQueue.offer(watchKey);
        dispatcher.start();
        verify(handler, timeout(5000L)).pathModified(eventDispatcher, file, true);
        sleep(SLEEP_TIME);
        verifyZeroInteractions(uncaughtExceptionHandler);
        verify(watchKey).reset();
    }

    @Test(timeout = 5000L)
    public void pathDiscardedExceptionOccurred() throws Exception {
        doThrow(exception).when(handler).pathDiscarded(eventDispatcher, file);
        when(watchEvent.kind()).thenReturn(ENTRY_DELETE);
        testQueue.offer(watchKey);
        dispatcher.start();
        verify(handler, timeout(5000L)).pathDiscarded(eventDispatcher, file);
        sleep(SLEEP_TIME);
        verifyZeroInteractions(uncaughtExceptionHandler);
        verify(watchKey).reset();
    }
}
