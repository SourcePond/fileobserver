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
package ch.sourcepond.io.fileobserver.impl.directory;

import org.slf4j.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.*;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.nio.file.StandardWatchEventKinds.*;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * <p>This thread-safe class manages watch-service instances and their associated watch-keys.
 */
class DirectoryScanner extends Thread implements Closeable {

    private static class DelayedWatchKey {
        private final long delayedUntilInMilliseconds;
        private final WatchKey key;

        DelayedWatchKey(final WatchKey pKey) {
            // TODO: Replace constant delay value with a configurable value.
            delayedUntilInMilliseconds = currentTimeMillis() + 2000;
            key = pKey;
        }

        public WatchKey getKey() {
            return key;
        }

        boolean isDue() {
            return 0 > delayedUntilInMilliseconds - currentTimeMillis();
        }
    }

    private static final Logger LOG = getLogger(DirectoryScanner.class);
    private final AtomicBoolean running = new AtomicBoolean(true);

    /**
     * We do <em>not</em> work with a {@link DelayQueue} here because the
     * timeout of the elements will always be linear. Furthermore, we do
     * not not need synchronization because the queue will always be accessed
     * from the same thread (runner thread).
     */
    private final List<DelayedWatchKey> delayQueue = new LinkedList<>();
    private final List<FsDirectories> roots;
    private final Directories directories;

    DirectoryScanner(final List<FsDirectories> pRoots, final Directories pDirectories) {
        super("sourcepond.DirectoryScanner");
        roots = pRoots;
        directories = pDirectories;
    }

    /**
     * Interrupts the worker thread which takes events from the managed
     * watch-service. Then, it delegates to the {@link WatchService#close()} method
     * of the managed watch-service.
     */
    @Override
    public void close() {
        if (running.compareAndSet(true, false)) {
            directories.close();
        }
    }

    private void processPath(final WatchEvent.Kind pKind, final Path child) {
        try {
            // The filename is the
            // context of the event.
            if (ENTRY_CREATE == pKind || ENTRY_MODIFY == pKind) {
                directories.pathModified(child);
            } else if (ENTRY_DELETE == pKind) {
                directories.pathDeleted(child);
            }
        } catch (final Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    private void processEvent(final WatchKey pWatchKey) throws IOException {
        final Path directory = (Path) pWatchKey.watchable();

        for (final WatchEvent<?> event : pWatchKey.pollEvents()) {
            final WatchEvent.Kind<?> kind = event.kind();

            if (LOG.isDebugEnabled()) {
                LOG.debug(format("Changed detected [%s]: %s, context: %s", kind, directory, event.context()));
            }

            // This key is registered only
            // for ENTRY_CREATE events,
            // but an OVERFLOW event can
            // occur regardless if events
            // are lost or discarded.
            if (OVERFLOW == kind) {
                continue;
            }

            // Only process if it's not a repeated event.
            if (event.count() == 1) {
                processPath(kind, directory.resolve((Path) event.context()));
            }
        }

        if (!pWatchKey.reset()) {
            directories.pathDeleted((Path) pWatchKey.watchable());
        }
    }

    private boolean queueWatchKeys() {
        FsDirectories next = null;
        WatchKey key;

        // We intentionally use a traditional for-loop to keep object creation
        // count as low as possible. Therefore, do not use an iterator here.
        for (int i = 0; i < roots.size(); i++) {
            try {
                next = roots.get(i);
                key = next.poll();
                if (null != key) {
                    delayQueue.add(new DelayedWatchKey(key));
                }
            } catch (final ClosedWatchServiceException e) {
                directories.close(next);
                LOG.debug(e.getMessage(), e);
            }
        }

        return !delayQueue.isEmpty();
    }

    private boolean processIfDue(final DelayedWatchKey delayedKey) {
        final boolean due = delayedKey.isDue();
        if (due) {
            try {
                processEvent(delayedKey.getKey());
            } catch (final IOException e) {
                LOG.warn(e.getMessage(), e);
            }
        }
        return due;
    }

    @Override
    public void run() {
        while (running.get() && !currentThread().isInterrupted()) {

            // Add new keys to the queue for delayed execution
            if (queueWatchKeys()) {

                // Process events if available
                delayQueue.removeIf(this::processIfDue);
            }
        }
    }
}
