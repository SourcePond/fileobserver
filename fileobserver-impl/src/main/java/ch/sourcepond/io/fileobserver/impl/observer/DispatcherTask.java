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

import ch.sourcepond.io.fileobserver.api.KeyDeliveryHook;
import ch.sourcepond.io.fileobserver.api.PathChangeListener;
import ch.sourcepond.io.fileobserver.impl.dispatch.KeyDeliveryConsumer;
import ch.sourcepond.io.fileobserver.impl.pending.PendingEventDone;
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
    private final ExecutorService observerExecutor;
    private final Collection<KeyDeliveryHook> hooks;
    private final Collection<PathChangeListener> observers;
    private final Consumer<PathChangeListener> fireEventConsumer;
    private final KeyDeliveryConsumer<T> beforeConsumer;
    private final KeyDeliveryConsumer<T> afterConsumer;
    private final T keyOrEvent;
    private final PendingEventDone doneHook;

    DispatcherTask(final ExecutorService pObserverExecutor,
                   final Collection<KeyDeliveryHook> pHooks,
                   final Collection<PathChangeListener> pObservers,
                   final T pKeyOrEvent,
                   final PendingEventDone pDoneHook,
                   final Consumer<PathChangeListener> pFireEventConsumer,
                   final KeyDeliveryConsumer<T> pBeforeConsumer,
                   final KeyDeliveryConsumer<T> pAfterConsumer) {
        observerExecutor = pObserverExecutor;
        hooks = pHooks;
        observers = pObservers;
        keyOrEvent = pKeyOrEvent;
        doneHook = pDoneHook;
        fireEventConsumer = pFireEventConsumer;
        beforeConsumer = pBeforeConsumer;
        afterConsumer = pAfterConsumer;
    }

    private void informHooks(final KeyDeliveryConsumer<T> pConsumer) {
        if (!hooks.isEmpty()) {
            final Collection<Future<?>> joins = new LinkedList<>();
            hooks.forEach(hook -> joins.add(observerExecutor.submit(() -> pConsumer.consume(hook, keyOrEvent))));
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
            pJoins.add(observerExecutor.submit(() ->
                    fireEventConsumer.accept(pObserver)));
        }
    }

    @Override
    public void run() {
        try {
            informHooks(beforeConsumer);
            final Collection<Future<?>> joins = new LinkedList<>();
            observers.forEach(observer -> submitObserverTask(observer, joins));
            joins.forEach(this::join);
            informHooks(afterConsumer);
        } finally {
            doneHook.done();
        }
    }
}
