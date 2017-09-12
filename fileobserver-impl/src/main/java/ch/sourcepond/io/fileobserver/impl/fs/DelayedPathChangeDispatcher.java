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

import ch.sourcepond.io.fileobserver.impl.Config;
import ch.sourcepond.io.fileobserver.impl.listener.ListenerManager;
import org.slf4j.Logger;

import java.io.Closeable;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;

import static java.lang.String.format;
import static java.lang.Thread.currentThread;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;
import static java.util.concurrent.Executors.newScheduledThreadPool;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.slf4j.LoggerFactory.getLogger;

/**
 *
 */
class DelayedPathChangeDispatcher implements Closeable {
    private static final Logger LOG = getLogger(DelayedPathChangeDispatcher.class);
    private final WatchServiceWrapper wrapper;
    private final PathChangeHandler pathChangeHandler;
    private final ListenerManager manager;
    private final Thread receiverThread;
    private final ScheduledExecutorService executor = newScheduledThreadPool(1);
    private final ConcurrentMap<Path, WatchEventQueue> queues = new ConcurrentHashMap<>();
    private volatile Config config;

    DelayedPathChangeDispatcher(final WatchServiceWrapper pWrapper,
                                final PathChangeHandler pPathChangeHandler,
                                final ListenerManager pManager) {
        wrapper = pWrapper;
        pathChangeHandler = pPathChangeHandler;
        manager = pManager;
        receiverThread = new Thread(this::receive, format("fs-event receiver %s", wrapper));
    }

    private void dispatchEvent(final Path pFile) {
        final WatchEventQueue queue = queues.remove(pFile);
        if (queue == null) {
            LOG.warn("No event queue found for {}, no events will be processed", pFile);
            return;
        }
        queue.processQueue(k -> {
            LOG.debug("Received event of kind {} for path {}", k, pFile);
            try {
                if (ENTRY_CREATE == k) {
                    pathChangeHandler.pathModified(manager.getDefaultDispatcher(), pFile, true);
                } else if (ENTRY_MODIFY == k) {
                    pathChangeHandler.pathModified(manager.getDefaultDispatcher(), pFile, false);
                } else if (ENTRY_DELETE == k) {
                    pathChangeHandler.pathDiscarded(manager.getDefaultDispatcher(), pFile);
                }
            } catch (final RuntimeException e) {
                LOG.error(e.getMessage(), e);
            }
        });
    }

    private void delayEvents(final WatchKey pKey) {
        final Path directory = (Path) pKey.watchable();
        for (final WatchEvent<?> event : pKey.pollEvents()) {
            final WatchEvent.Kind<?> kind = event.kind();
            LOG.debug("Changed detected [{}]: {}, context: {}", kind, directory, event.context());

            // An OVERFLOW event can
            // occur regardless if events
            // are lost or discarded.
            if (OVERFLOW == kind) {
                continue;
            }

            final Path file = directory.resolve((Path) event.context());
            queues.computeIfAbsent(file, f -> {
                final WatchEventQueue q = new WatchEventQueue();
                executor.schedule(() -> dispatchEvent(f), config.eventDispatchDelayMillis(), MILLISECONDS);
                return q;

            }).push(kind);
        }
    }

    public void start() {
        receiverThread.setDaemon(true);
        receiverThread.start();
    }

    /**
     * <p>Stops the scanner threads which observe the watched directories for changes.</p>
     * <p>
     * <p>This must be named "stop" in order to be called from Felix DM (see
     * <a href="http://felix.apache.org/documentation/subprojects/apache-felix-dependency-manager/reference/components.html">Dependency Manager - Components</a>)</p>
     */
    @Override
    public void close() {
        receiverThread.interrupt();
        executor.shutdown();
        wrapper.close();
    }

    private void receive() {
        while (!currentThread().isInterrupted()) {
            try {
                final WatchKey key = wrapper.take();
                try {
                    delayEvents(key);
                } finally {
                    key.reset();
                }
            } catch (final InterruptedException e) {
                LOG.warn(e.getMessage(), e);
                break;
            }
        }
    }

    public void setConfig(Config pConfig) {
        config = pConfig;
    }
}
