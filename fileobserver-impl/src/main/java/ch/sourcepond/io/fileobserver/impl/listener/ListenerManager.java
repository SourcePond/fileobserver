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

import ch.sourcepond.io.fileobserver.api.DispatchKey;
import ch.sourcepond.io.fileobserver.api.KeyDeliveryHook;
import ch.sourcepond.io.fileobserver.api.PathChangeEvent;
import ch.sourcepond.io.fileobserver.api.PathChangeListener;
import ch.sourcepond.io.fileobserver.impl.Config;
import ch.sourcepond.io.fileobserver.impl.dispatch.KeyDeliveryConsumer;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * This class handles everything necessary to inform registered {@link PathChangeListener} and
 * {@link KeyDeliveryHook} instances.
 */
public class ListenerManager implements ReplayDispatcher {
    private static final Logger LOG = getLogger(ListenerManager.class);
    private final DefaultDispatchRestrictionFactory restrictionFactory;
    private final DispatchEventFactory dispatchEventFactory;
    private final Set<KeyDeliveryHook> hooks = new CopyOnWriteArraySet<>();
    private final ConcurrentMap<PathChangeListener, Map<FileSystem, DefaultDispatchRestriction>> listeners = new ConcurrentHashMap<>();
    private final EventDispatcher defaultDispatcher = new EventDispatcher(this, listeners.keySet());
    private volatile Executor dispatcherExecutor;
    private volatile ExecutorService listenerExecutor;
    private volatile Config config;

    // Constructor for activator
    public ListenerManager() {
        this(new DefaultDispatchRestrictionFactory(), new DispatchEventFactory());
    }

    // Constructor for testing
    ListenerManager(final DefaultDispatchRestrictionFactory pRestrictionFactory,
                    final DispatchEventFactory pDispatchEventFactory) {
        restrictionFactory = pRestrictionFactory;
        dispatchEventFactory = pDispatchEventFactory;
    }

    public EventDispatcher addListener(final PathChangeListener pListener) {
        listeners.computeIfAbsent(pListener, o -> new ConcurrentHashMap<>());
        return new EventDispatcher(this, pListener);
    }

    public DiffEventDispatcher openDiff(final DedicatedFileSystem pFs) {
        return new DiffEventDispatcher(this, new DiffListener(pFs, defaultDispatcher, config));
    }

    public EventDispatcher getDefaultDispatcher() {
        return defaultDispatcher;
    }

    Collection<PathChangeListener> getListeners() {
        return listeners.keySet();
    }

    public void setConfig(final Config pConfig) {
        config = pConfig;
    }

    public void setExecutors(final ExecutorService pDispatcherExecutor, final ExecutorService pListenerExecutor) {
        dispatcherExecutor = pDispatcherExecutor;
        listenerExecutor = pListenerExecutor;
    }

    public void addHook(final KeyDeliveryHook pHook) {
        hooks.add(pHook);
    }

    public void removeObserver(final PathChangeListener pListener) {
        listeners.remove(pListener);
    }

    public void removeHook(final KeyDeliveryHook pHook) {
        hooks.remove(pHook);
    }

    private static void fireModification(final PathChangeListener pListener,
                                         final PathChangeEvent pEvent,
                                         final Collection<DispatchKey> pParentKeys) {
        final DispatchKey key = pEvent.getKey();
        if (!pParentKeys.isEmpty()) {
            for (final DispatchKey parentKey : pParentKeys) {
                /*
                 * Suppose:
                 * Parent /A [dirKey:K2] -> Has been added as new root
                 *    Child /A/B [dirKey:K2] -> Derived from new root = nothing to supplement
                 *               [dirKey:K1] -> Was there before new root had been added = A/B supplements B
                 *
                 * When iterating over parent keys ignore those which are derived from new parent.
                 *
                 */
                if (!key.getDirectoryKey().equals(parentKey.getDirectoryKey())) {
                    pListener.supplement(key, parentKey);
                }
            }
        }
        try {
            pListener.modified(pEvent);
        } catch (final IOException e) {
            LOG.warn(e.getMessage(), e);
        }
    }

    private void fireModification(final PathChangeListener pListener,
                                  final DispatchKey pKey,
                                  final Path pFile,
                                  final Collection<DispatchKey> pParentKeys) {
        fireModification(pListener,
                dispatchEventFactory.create(pListener, pKey, pFile, pParentKeys, this),
                pParentKeys);
    }

    private DefaultDispatchRestriction createRestriction(final PathChangeListener pListener, final FileSystem pFs) {
        final DefaultDispatchRestriction restriction = restrictionFactory.createRestriction(pFs);
        pListener.restrict(restriction, pFs);
        return restriction;
    }

    private boolean isAccepted(final PathChangeListener pListener, final DispatchKey pDispatchKey) {
        final FileSystem fs = pDispatchKey.getRelativePath().getFileSystem();
        return listeners.computeIfAbsent(
                pListener, o -> new ConcurrentHashMap<>()).
                computeIfAbsent(fs, f -> createRestriction(pListener, f)).isAccepted(pDispatchKey);
    }

    private <T> void submitTask(final Collection<PathChangeListener> pListeners,
                                final T pKeyOrEvent,
                                final Consumer<PathChangeListener> pFireEventConsumer,
                                final KeyDeliveryConsumer<T> pBeforeConsumer,
                                final KeyDeliveryConsumer<T> pAfterConsumer) {
        dispatcherExecutor.execute(new DispatcherTask(
                listenerExecutor,
                hooks,
                pListeners,
                pKeyOrEvent,
                pFireEventConsumer,
                pBeforeConsumer,
                pAfterConsumer
        ));
    }

    private void submitDispatchTask(final Collection<PathChangeListener> pListeners,
                                    final DispatchKey pKey,
                                    final Consumer<PathChangeListener> pFireEventConsumer,
                                    final KeyDeliveryConsumer<DispatchKey> pBeforeConsumer,
                                    final KeyDeliveryConsumer<DispatchKey> pAfterConsumer) {
        final Collection<PathChangeListener> acceptingListeners = pListeners.stream().filter(
                o -> isAccepted(o, pKey)).collect(toList());
        if (!acceptingListeners.isEmpty()) {
            submitTask(acceptingListeners,
                    pKey,
                    pFireEventConsumer,
                    pBeforeConsumer,
                    pAfterConsumer
            );
        }
    }

    @Override
    public void replay(final PathChangeListener pListener,
                       final PathChangeEvent pEvent,
                       final Collection<DispatchKey> pParentKeys) {
        submitTask(asList(pListener),
                pEvent,
                observer -> fireModification(pListener, pEvent, pParentKeys),
                (hook, event) -> hook.beforeModify(event.getKey(), event.getFile()),
                (hook, event) -> hook.afterModify(event.getKey(), event.getFile())
        );
    }

    void modified(final Collection<PathChangeListener> pListeners, final DispatchKey pKey, final Path pFile, final Collection<DispatchKey> pParentKeys) {
        submitDispatchTask(
                pListeners,
                pKey,
                observer -> fireModification(observer, pKey, pFile, pParentKeys),
                (hook, key) -> hook.beforeModify(key, pFile),
                (hook, key) -> hook.afterModify(key, pFile)
        );
    }

    void discard(final Collection<PathChangeListener> pListeners, final DispatchKey pKey) {
        submitDispatchTask(
                pListeners,
                pKey,
                observer -> observer.discard(pKey),
                (hook, key) -> hook.beforeDiscard(key),
                (hook, key) -> hook.afterDiscard(key)
        );
    }

    public void removeFileSystem(final FileSystem pFs) {
        listeners.values().forEach(m -> m.remove(pFs));
    }
}
