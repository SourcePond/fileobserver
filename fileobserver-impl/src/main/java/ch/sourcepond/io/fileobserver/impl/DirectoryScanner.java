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
package ch.sourcepond.io.fileobserver.impl;

import org.slf4j.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.String.format;
import static java.lang.Thread.currentThread;
import static java.nio.file.StandardWatchEventKinds.*;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * <p>This thread-safe class manages watch-service instances and their associated watch-keys.
 */
class DirectoryScanner implements Runnable, Closeable, WatchKeyProcessor {
    private static final Logger LOG = getLogger(DirectoryScanner.class);
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final ExecutorService executor;
    private final Directories directories;

    DirectoryScanner(final ExecutorService pExecutor, final Directories pDirectories) {
        executor = pExecutor;
        directories = pDirectories;
    }

    void start() {
        executor.execute(this);
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
        // The filename is the
        // context of the event.
        if (ENTRY_CREATE == pKind || ENTRY_MODIFY == pKind) {
            directories.pathCreated(child);
        } else if (ENTRY_DELETE == pKind)  {
            directories.pathDeleted(child);
        }
    }

    @Override
    public void processEvent(final WatchKey pWatchKey) throws IOException {
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

            processPath(kind, directory.resolve((Path) event.context()));
        }

        if (!pWatchKey.reset()) {
            directories.removeInvalidDirectory((Path) pWatchKey.watchable());
        }
    }

    @Override
    public void run() {
        while (running.get() && !currentThread().isInterrupted()) {
            directories.processFsEvents(this);
        }
    }
}
