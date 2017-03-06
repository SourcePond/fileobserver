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

import ch.sourcepond.io.fileobserver.impl.fs.DedicatedFileSystem;
import ch.sourcepond.io.fileobserver.impl.fs.VirtualRoot;
import org.slf4j.Logger;

import java.nio.file.*;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;
import static java.lang.Thread.currentThread;
import static java.nio.file.StandardWatchEventKinds.*;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * <p>This thread-safe class manages watch-service instances and their associated watch-keys.
 */
public class DirectoryScanner implements Runnable {
    // TODO: Replace constant delay value with a configurable value.
    private static final int TIMEOUT = 2000;
    private static final Logger LOG = getLogger(DirectoryScanner.class);
    private final Clock clock;
    private final VirtualRoot virtualRoot;
    private final Thread thread = new Thread(this, "fileobserver.DirectoryScanner");

    // Constructor for testing and BundleActivator
    public DirectoryScanner(final Clock pClock, final VirtualRoot pVirtualRoot) {
        clock = pClock;
        virtualRoot = pVirtualRoot;
    }

    // Lifecycle method for Felix DM
    public void start() {
        thread.setDaemon(true);
        thread.start();
        LOG.info("Directory scanner started");
    }

    /**
     * Interrupts the worker thread which takes events from the managed
     * watch-service. Then, it delegates to the {@link WatchService#close()} method
     * of the managed watch-service.
     */
    // Lifecycle method for Felix DM
    public void stop() {
        thread.interrupt();
        LOG.info("Directory scanner stopped");
    }

    private void processPath(final WatchEvent.Kind<?> pKind, final Path child) {
        try {
            // The filename is the
            // context of the event.
            if (ENTRY_CREATE == pKind || ENTRY_MODIFY == pKind) {
                virtualRoot.pathModified(child);
            } else if (ENTRY_DELETE == pKind) {
                virtualRoot.pathDeleted(child);
            }
        } catch (final Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    private void processEvent(final WatchKey pWatchKey) {
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
            virtualRoot.pathDeleted((Path) pWatchKey.watchable());
        }
    }

    private boolean waitForNextIteration() {
        synchronized (this) {
            long nextRun = clock.millis() + TIMEOUT;
            while (nextRun > clock.millis()) {
                try {
                    wait(TIMEOUT);
                } catch (final InterruptedException e) {
                    currentThread().interrupt();
                }
            }
        }
        return !currentThread().isInterrupted();
    }

    @Override
    public void run() {
        // Avoid accessing instance method to often
        final List<DedicatedFileSystem> roots = virtualRoot.getRoots();
        final List<WatchKey> keys = new ArrayList<>();

        DedicatedFileSystem next = null;
        WatchKey key;

        while (waitForNextIteration()) {
            // We intentionally use a traditional for-loop to keep object creation
            // count as low as possible. Therefore, do not use an iterator here.
            for (int i = 0 ; i < keys.size() ; i++) {
                processEvent(keys.get(i));
            }

            // We intentionally use a traditional for-loop to keep object creation
            // count as low as possible. Therefore, do not use an iterator here.
            for (int i = 0; i < roots.size(); i++) {
                try {
                    next = roots.get(i);
                    key = next.poll();
                    if (key != null) {
                        keys.add(key);
                    }
                } catch (final ClosedWatchServiceException e) {
                    virtualRoot.close(next);
                    LOG.debug(e.getMessage(), e);
                }
            }
        }
    }
}
