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

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

import static java.util.Collections.emptyList;
import static java.util.concurrent.ConcurrentHashMap.newKeySet;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * This class handles everything necessary to inform registered {@link FileObserver} and
 * {@link KeyDeliveryHook} instances.
 */
public class ObserverDispatcher {
    private static final Logger LOG = getLogger(ObserverDispatcher.class);
    private final Set<KeyDeliveryHook> hooks = newKeySet();
    private final Set<FileObserver> observers = newKeySet();
    private final DispatcherTaskFactory taskFactory;
    private Executor dispatcherExecutor;
    private ExecutorService observerExecutor;

    // Constructor for activator
    ObserverDispatcher() {
        this(new DispatcherTaskFactory());
    }

    // Constructor for testing
    ObserverDispatcher(final DispatcherTaskFactory pTaskFactory) {
        taskFactory = pTaskFactory;
    }

    void setDispatcherExecutor(final ExecutorService pDispatcherExecutor) {
        dispatcherExecutor = pDispatcherExecutor;
    }

    void setObserverExecutor(final ExecutorService pObserverExecutor) {
        observerExecutor = pObserverExecutor;
    }

    void addObserver(final FileObserver pObserver) {
        observers.add(pObserver);
    }

    void addHook(final KeyDeliveryHook pHook) {
        hooks.add(pHook);
    }

    void removeObserver(final FileObserver pObserver) {
        observers.remove(pObserver);
    }

    void removeHook(final KeyDeliveryHook pHook) {
        hooks.remove(pHook);
    }

    private static void fireModification(final FileObserver pObserver,
                                  final FileKey pKey,
                                  final Path pFile,
                                  final Collection<FileKey> pParentKeys) {
        if (!pParentKeys.isEmpty()) {
            for (final FileKey parentKey : pParentKeys) {
                /*
                 * Suppose:
                 * Parent /A [dirKey:K2] -> Has been added as new root
                 *    Child /A/B [dirKey:K2] -> Derived from new root = nothing to supplement
                 *               [dirKey:K1] -> Was there before new root had been added = A/B supplements B
                 *
                 * When iterating over parent keys ignore those which are derived from new parent.
                 *
                 */
                if (!pKey.directoryKey().equals(parentKey.directoryKey())) {
                    pObserver.supplement(pKey, parentKey);
                }
            }
        }
        try {
            pObserver.modified(pKey, pFile);
        } catch (final IOException e) {
            LOG.warn(e.getMessage(), e);
        }
    }

    private void submitTask(final FileKey pKey,
                            final Consumer<FileObserver> pFireEventConsumer,
                            final KeyDeliveryConsumer pBeforeConsumer,
                            final KeyDeliveryConsumer pAfterConsumer,
                            final Collection<FileKey> pParentKeys) {
        if (!observers.isEmpty()) {
            dispatcherExecutor.execute(new DispatcherTask(
                    observerExecutor,
                    hooks,
                    observers,
                    pKey,
                    pFireEventConsumer,
                    pBeforeConsumer,
                    pAfterConsumer,
                    pParentKeys
            ));
        }
    }

    public void modified(final FileKey pKey, final Path pFile, final Collection<FileKey> pParentKeys) {
        submitTask(
                pKey,
                observer -> fireModification(observer, pKey, pFile, pParentKeys),
                (hook, key) -> hook.beforeModify(key),
                (hook, key) -> hook.afterModify(key),
                pParentKeys
        );
    }

    public void discard(final FileKey pKey) {
        submitTask(
                pKey,
                observer -> observer.discard(pKey),
                (hook, key) -> hook.beforeDiscard(key),
                (hook, key) -> hook.afterDiscard(key),
                emptyList()
        );
    }
}
