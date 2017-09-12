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
import ch.sourcepond.io.fileobserver.impl.directory.Directory;
import ch.sourcepond.io.fileobserver.impl.listener.ListenerManager;
import org.slf4j.Logger;

import java.io.Closeable;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;

import static java.lang.String.format;
import static java.nio.file.Files.isDirectory;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.Executors.newScheduledThreadPool;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.slf4j.LoggerFactory.getLogger;

/**
 *
 */
class FsEventDispatcher implements Closeable {
    private static final Logger LOG = getLogger(FsEventDispatcher.class);
    private final ScheduledExecutorService executor = newScheduledThreadPool(1);
    private final WatchServiceWrapper wrapper;
    private final ListenerManager manager;
    private final DirectoryRegistrationWalker walker;
    private final ConcurrentMap<Path, Directory> dirs;
    final ConcurrentMap<Path, WatchEventQueue> queues = new ConcurrentHashMap<>();
    final Thread receiverThread;
    private volatile Config config;

    FsEventDispatcher(final ConcurrentMap<Path, Directory> pDirs,
                      final DirectoryRegistrationWalker pWalker,
                      final WatchServiceWrapper pWrapper,
                      final ListenerManager pManager) {
        walker = pWalker;
        dirs = pDirs;
        wrapper = pWrapper;
        manager = pManager;
        receiverThread = new Thread(this::receive, format("fs-event dispatcher %s", wrapper));
    }

    private void receive() {
        while (!receiverThread.isInterrupted()) {
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

    private Directory getDirectory(final Path pPath) {
        return dirs.get(pPath);
    }

    private void pathModified(final Path pPath,
                              final boolean pIsCreated) {
        // We are only interested in directories when they have been created...
        if (isDirectory(pPath)) {
            if (pIsCreated) {
                walker.directoryCreated(manager.getDefaultDispatcher(), pPath);
            }
            //...otherwise, ignore them
        } else {
            final Directory dir = requireNonNull(getDirectory(pPath.getParent()),
                    () -> format("No directory registered for %s", pPath));
            dir.informIfChanged(manager.getDefaultDispatcher(), pPath, pIsCreated);
        }
    }

    private void pathDiscarded(final Path pPath) {
        // The deleted path was a directory
        if (!directoryDiscarded(pPath)) {
            final Directory parentDirectory = getDirectory(pPath.getParent());
            if (parentDirectory == null) {
                LOG.debug("Parent of {} does not exist. Nothing to discard", pPath);
            } else {
                // The deleted path was a file
                parentDirectory.informDiscard(manager.getDefaultDispatcher(), pPath);
            }
        }
    }

    private void processPath(final WatchEvent.Kind<?> pKind, final Path pPath) {
        LOG.debug("Received event of kind {} for path {}", pKind, pPath);
        try {
            if (ENTRY_CREATE == pKind) {
                pathModified(pPath, true);
            } else if (ENTRY_MODIFY == pKind) {
                pathModified(pPath, false);
            } else if (ENTRY_DELETE == pKind) {
                pathDiscarded(pPath);
            }
        } catch (final RuntimeException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    private void directoryDiscarded(final Path pPath, final Directory dir) {
        dir.cancelKeyAndDiscardResources(manager.getDefaultDispatcher());

        for (final Iterator<Map.Entry<Path, Directory>> it = dirs.entrySet().iterator(); it.hasNext(); ) {
            final Map.Entry<Path, Directory> entry = it.next();

            final Path subPath = entry.getKey();
            if (subPath.startsWith(pPath)) {
                it.remove();
                directoryDiscarded(subPath, entry.getValue());
            }
        }
    }

    private boolean directoryDiscarded(final Path pDirectory) {
        final Directory dir = dirs.remove(pDirectory);
        final boolean wasDirectory = dir != null;
        if (wasDirectory) {
            directoryDiscarded(pDirectory, dir);
        }
        return wasDirectory;
    }

    private void dispatchEvent(final Path pPath) {
        queues.computeIfPresent(pPath, (path, queue) -> {
            queue.processQueue(kind -> processPath(kind, path));
            return null;
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
        assert config != null : "config is null";
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

    public void setConfig(Config pConfig) {
        config = pConfig;
    }
}
