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
import ch.sourcepond.io.fileobserver.impl.Config;
import ch.sourcepond.io.fileobserver.impl.filekey.KeyDeliveryConsumer;
import ch.sourcepond.io.fileobserver.impl.fs.DedicatedFileSystem;
import ch.sourcepond.io.fileobserver.impl.restriction.DefaultDispatchRestriction;
import ch.sourcepond.io.fileobserver.impl.restriction.DefaultDispatchRestrictionFactory;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.function.Consumer;

import static java.util.stream.Collectors.toList;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * This class handles everything necessary to inform registered {@link FileObserver} and
 * {@link KeyDeliveryHook} instances.
 */
public class ObserverManager {
    private static final Logger LOG = getLogger(ObserverManager.class);
    private final DefaultDispatchRestrictionFactory restrictionFactory;
    private final Set<KeyDeliveryHook> hooks = new CopyOnWriteArraySet<>();
    private final ConcurrentMap<FileObserver, Map<FileSystem, DefaultDispatchRestriction>> observers = new ConcurrentHashMap<>();
    private final EventDispatcher defaultDispatcher = new EventDispatcher(this, observers.keySet());
    private volatile Executor dispatcherExecutor;
    private volatile ExecutorService observerExecutor;
    private volatile Config config;

    // Constructor for activator
    public ObserverManager() {
        this(new DefaultDispatchRestrictionFactory());
    }

    // Constructor for testing
    ObserverManager(final DefaultDispatchRestrictionFactory pRestrictionFactory) {
        restrictionFactory = pRestrictionFactory;
    }

    public EventDispatcher addObserver(final FileObserver pObserver) {
        observers.computeIfAbsent(pObserver, o -> new ConcurrentHashMap<>());
        return new EventDispatcher(this, pObserver);
    }

    public DiffEventDispatcher openDiff(final DedicatedFileSystem pFs) {
        return new DiffEventDispatcher(this, new DiffObserver(pFs, defaultDispatcher, config));
    }

    public EventDispatcher getDefaultDispatcher() {
        return defaultDispatcher;
    }

    Collection<FileObserver> getObservers() {
        return observers.keySet();
    }

    public void setConfig(final Config pConfig) {
        config = pConfig;
    }

    public void setDispatcherExecutor(final ExecutorService pDispatcherExecutor) {
        dispatcherExecutor = pDispatcherExecutor;
    }

    public void setObserverExecutor(final ExecutorService pObserverExecutor) {
        observerExecutor = pObserverExecutor;
    }

    public void addHook(final KeyDeliveryHook pHook) {
        hooks.add(pHook);
    }

    public void removeObserver(final FileObserver pObserver) {
        observers.remove(pObserver);
    }

    public void removeHook(final KeyDeliveryHook pHook) {
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
                if (!pKey.getDirectoryKey().equals(parentKey.getDirectoryKey())) {
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

    private DefaultDispatchRestriction createRestriction(final FileObserver pObserver, final FileSystem pFs) {
        final DefaultDispatchRestriction restriction = restrictionFactory.createRestriction(pFs);
        pObserver.setup(restriction);
        return restriction;
    }

    private boolean isAccepted(final FileObserver pObserver, final FileKey pFileKey) {
        final FileSystem fs = pFileKey.getRelativePath().getFileSystem();
        return observers.computeIfAbsent(
                pObserver, o -> new ConcurrentHashMap<>()).
                computeIfAbsent(fs, f -> createRestriction(pObserver, f)).isAccepted(pFileKey);
    }

    private void submitTask(final Collection<FileObserver> pObservers,
                            final FileKey pKey,
                            final Consumer<FileObserver> pFireEventConsumer,
                            final KeyDeliveryConsumer pBeforeConsumer,
                            final KeyDeliveryConsumer pAfterConsumer) {
        final Collection<FileObserver> acceptingObservers = pObservers.stream().filter(
                o -> isAccepted(o, pKey)).collect(toList());
        if (!acceptingObservers.isEmpty()) {
            dispatcherExecutor.execute(new DispatcherTask(
                    observerExecutor,
                    hooks,
                    acceptingObservers,
                    pKey,
                    pFireEventConsumer,
                    pBeforeConsumer,
                    pAfterConsumer
            ));
        }
    }

    void modified(final Collection<FileObserver> pObservers, final Collection<FileKey> pKeys, final Path pFile, final Collection<FileKey> pParentKeys) {
        pKeys.forEach(key -> modified(pObservers, key, pFile, pParentKeys));
    }

    void modified(final Collection<FileObserver> pObservers, final FileKey pKey, final Path pFile, final Collection<FileKey> pParentKeys) {
        submitTask(
                pObservers,
                pKey,
                observer -> fireModification(observer, pKey, pFile, pParentKeys),
                (hook, key) -> hook.beforeModify(key, pFile),
                (hook, key) -> hook.afterModify(key, pFile)
        );
    }

    void discard(final Collection<FileObserver> pObservers, final FileKey pKey) {
        submitTask(
                pObservers,
                pKey,
                observer -> observer.discard(pKey),
                (hook, key) -> hook.beforeDiscard(key),
                (hook, key) -> hook.afterDiscard(key)
        );
    }

    public void removeFileSystem(final FileSystem pFs) {
        observers.values().forEach(m -> m.remove(pFs));
    }
}
