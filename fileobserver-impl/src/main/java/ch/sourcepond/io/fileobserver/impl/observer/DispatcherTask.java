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
import ch.sourcepond.io.fileobserver.impl.filekey.KeyDeliveryConsumer;
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
class DispatcherTask implements Runnable {
    private static final Logger LOG = getLogger(DispatcherTask.class);
    private final ExecutorService observerExecutor;
    private final Collection<KeyDeliveryHook> hooks;
    private final Collection<FileObserver> observers;
    private final Collection<FileKey> parentKeys;
    private final Consumer<FileObserver> fireEventConsumer;
    private final KeyDeliveryConsumer beforeConsumer;
    private final KeyDeliveryConsumer afterConsumer;
    private final FileKey key;

    DispatcherTask(final ExecutorService pObserverExecutor,
                   final Collection<KeyDeliveryHook> pHooks,
                   final Collection<FileObserver> pObservers,
                   final FileKey pKey,
                   final Consumer<FileObserver> pFireEventConsumer,
                   final KeyDeliveryConsumer pBeforeConsumer,
                   final KeyDeliveryConsumer pAfterConsumer,
                   final Collection<FileKey> pParentKeys) {
        observerExecutor = pObserverExecutor;
        hooks = pHooks;
        observers = pObservers;
        key = pKey;
        fireEventConsumer = pFireEventConsumer;
        beforeConsumer = pBeforeConsumer;
        afterConsumer = pAfterConsumer;
        parentKeys = pParentKeys;
    }

    private void informHooks(final KeyDeliveryConsumer pConsumer) {
        if (!hooks.isEmpty()) {
            final Collection<Future<?>> joins = new LinkedList<>();
            hooks.forEach(hook -> joins.add(observerExecutor.submit(() -> pConsumer.consume(hook, key))));
            joins.forEach(j -> join(j));
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

    private void submitObserverTask(final FileObserver pObserver, final Collection<Future<?>> pJoins) {
        if (!currentThread().isInterrupted()) {
            pJoins.add(observerExecutor.submit(() ->
                    fireEventConsumer.accept(pObserver)));
        }
    }

    @Override
    public void run() {
        informHooks(beforeConsumer);
        final Collection<Future<?>> joins = new LinkedList<>();
        observers.forEach(observer -> submitObserverTask(observer, joins));
        joins.forEach(j -> join(j));
        informHooks(afterConsumer);
    }
}
