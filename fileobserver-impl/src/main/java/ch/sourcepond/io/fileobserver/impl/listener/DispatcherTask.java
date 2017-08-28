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

import ch.sourcepond.io.fileobserver.api.KeyDeliveryHook;
import ch.sourcepond.io.fileobserver.api.PathChangeListener;
import ch.sourcepond.io.fileobserver.impl.dispatch.KeyDeliveryConsumer;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import static java.lang.Thread.currentThread;
import static org.slf4j.LoggerFactory.getLogger;

/**
 *
 */
class DispatcherTask<T> implements Runnable {
    private static final Logger LOG = getLogger(DispatcherTask.class);
    private final ExecutorService listenerExecutor;
    private final Collection<KeyDeliveryHook> hooks;
    private final Collection<PathChangeListener> listeners;
    private final Consumer<PathChangeListener> fireEventConsumer;
    private final KeyDeliveryConsumer<T> beforeConsumer;
    private final KeyDeliveryConsumer<T> afterConsumer;
    private final T keyOrEvent;
    private final Runnable doneHook;

    DispatcherTask(final ExecutorService pListenerExecutor,
                   final Collection<KeyDeliveryHook> pHooks,
                   final Collection<PathChangeListener> pListeners,
                   final T pKeyOrEvent,
                   final Runnable pDoneHook,
                   final Consumer<PathChangeListener> pFireEventConsumer,
                   final KeyDeliveryConsumer<T> pBeforeConsumer,
                   final KeyDeliveryConsumer<T> pAfterConsumer) {
        listenerExecutor = pListenerExecutor;
        hooks = pHooks;
        listeners = pListeners;
        keyOrEvent = pKeyOrEvent;
        doneHook = pDoneHook;
        fireEventConsumer = pFireEventConsumer;
        beforeConsumer = pBeforeConsumer;
        afterConsumer = pAfterConsumer;
    }

    private void informHooks(final KeyDeliveryConsumer<T> pConsumer) {
        if (!hooks.isEmpty()) {
            final Collection<Future<?>> joins = new LinkedList<>();
            hooks.forEach(hook -> joins.add(listenerExecutor.submit(() -> pConsumer.consume(hook, keyOrEvent))));
            joins.forEach(this::join);
        }
    }

    private void join(final Future<?> pJoin) {
        try {
            pJoin.get();
        } catch (final InterruptedException e) {
            currentThread().interrupt();
        } catch (final ExecutionException e) {
            LOG.warn(e.getMessage(), e);
        }
    }

    private void submitObserverTask(final PathChangeListener pObserver, final Collection<Future<?>> pJoins) {
        if (!currentThread().isInterrupted()) {
            pJoins.add(listenerExecutor.submit(() ->
                    fireEventConsumer.accept(pObserver)));
        }
    }

    @Override
    public void run() {
        try {
            informHooks(beforeConsumer);
            final Collection<Future<?>> joins = new LinkedList<>();
            listeners.forEach(observer -> submitObserverTask(observer, joins));
            joins.forEach(this::join);
            informHooks(afterConsumer);
        } finally {
            doneHook.run();
        }
    }
}
