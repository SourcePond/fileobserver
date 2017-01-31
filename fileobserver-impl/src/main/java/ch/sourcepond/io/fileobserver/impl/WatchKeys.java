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

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;

import static java.lang.String.format;
import static java.lang.Thread.currentThread;
import static java.nio.file.StandardWatchEventKinds.*;
import static java.util.Collections.emptyList;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * <p>This thread-safe class manages watch-service instances and their associated watch-keys.
 */
class WatchKeys implements Runnable {
    private static final Logger LOG = getLogger(WatchKeys.class);
    private final Map<Path, WatchKey> keys = new HashMap<>();

    // This field is also used in the run() method; because this
    // it should be of type ConcurrentMap to avoid locking overhead.
    private final ConcurrentMap<FileSystem, WatchService> watchServices = new ConcurrentHashMap<>();
    private final ResourceEventProducer resourceEventProducer;
    private final ExecutorService executor;
    private volatile boolean running = true;

    WatchKeys(final ExecutorService pExecutor, final ResourceEventProducer pResourceEventProducer) {
        executor = pExecutor;
        resourceEventProducer = pResourceEventProducer;
    }

    void start() {
        executor.execute(this);
    }

    /**
     * Interrupts the worker thread which takes events from the managed
     * watch-service. Then, it delegates to the {@link WatchService#close()} method
     * of the managed watch-service.
     */
    void shutdown() {
        synchronized (keys) {
            if (running) {
                running = false;
                for (final Iterator<WatchService> it = watchServices.values().iterator(); it.hasNext(); ) {
                    try {
                        it.next().close();
                    } catch (final IOException e) {
                        LOG.warn(e.getMessage(), e);
                    }
                    it.remove();
                }
                keys.clear();
            }
        }
    }

    private boolean doCancelWatchKey(final Path pDirectory) {
        final Collection<WatchKey> keysToBeCancelled;
        synchronized (keys) {
            if (keys.containsKey(pDirectory)) {
                keysToBeCancelled = new LinkedList<>();
                keysToBeCancelled.add(keys.remove(pDirectory));
                keys.entrySet().removeIf(e -> {
                    if (e.getKey().startsWith(pDirectory)) {
                        keysToBeCancelled.add(e.getValue());
                    }
                    return true;
                });
            } else {
                keysToBeCancelled = emptyList();
            }
        }
        if (!keysToBeCancelled.isEmpty()) {
            for (final WatchKey keyToBeCancelled : keysToBeCancelled) {
                keyToBeCancelled.cancel();
                if (LOG.isDebugEnabled()) {
                    LOG.debug(format("Cancelled watch-key of directory %s", keyToBeCancelled.watchable()));
                }
            }
            return true;
        } else if (LOG.isDebugEnabled()) {
            LOG.debug(format("No watch-key was registered for path %s", pDirectory));
        }
        return false;
    }

    private WatchService newWatchService(final FileSystem pFs) {
        try {
            return pFs.newWatchService();
        } catch (final IOException e) {
            throw new WatchServiceException(e);
        }
    }

    private WatchService getWatchService(final FileSystem pFs) throws IOException {
        try {
            return watchServices.computeIfAbsent(pFs,
                    this::newWatchService);
        } catch (final WatchServiceException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    /**
     * Cancels the watch-key of the directory specified and removes its from
     * the managed watch-keys.
     *
     * @param pDirectory Directory, must not be {@code null}
     * @return This object, never {@code null}
     */
    WatchKeys cancelWatchKey(final Path pDirectory) {
        doCancelWatchKey(pDirectory);
        return this;
    }

    private void installWatchService(final WatchService pWatchService, final Path pDirectory) throws IOException {
        keys.putAll(new WatchServiceInstaller(pWatchService, pDirectory).registerDirectories());
    }

    /**
     * Registers the directory specified with the watch-service of this object. The
     * path must have the same {@link java.nio.file.FileSystem} as the managed watch-service.
     *
     * @param pDirectory Directory, must not be {@code null}
     * @throws IOException Thrown, if something went wrong during registration.
     */
    void openWatchKey(final Path pDirectory) throws IOException {
        synchronized (keys) {
            if (!keys.containsKey(pDirectory)) {
                installWatchService(getWatchService(pDirectory.getFileSystem()), pDirectory);
            }
        }
    }

    private void resetKey(final WatchKey watchKey) {
        if (!watchKey.reset()) {
            doCancelWatchKey((Path) watchKey.watchable());
        }
    }

    private void produceResourceEvent(final WatchService watchService, final WatchEvent.Kind kind, final Path child) {
        final File childFile = child.toFile();
        if (childFile.isDirectory() && ENTRY_CREATE == kind) {
            try {
                installWatchService(watchService, child);
            } catch (final IOException e) {
                LOG.warn(e.getMessage(), e);
            }
        } else if (childFile.isFile() && (ENTRY_CREATE == kind || ENTRY_MODIFY == kind)) {
            resourceEventProducer.fileModify(child);
        } else if (childFile.isFile() && (ENTRY_DELETE == kind && !doCancelWatchKey(child))) {
            resourceEventProducer.fileDelete(child);
        }
    }

    private void processWatchKey(final WatchService watchService, final WatchKey watchKey) {
        if (null != watchKey) {
            final Path directory = (Path) watchKey.watchable();

            for (final WatchEvent<?> event : watchKey.pollEvents()) {
                final WatchEvent.Kind<?> kind = event.kind();

                if (LOG.isDebugEnabled()) {
                    LOG.debug(format("Changed detected [%s]: %s", kind, directory));
                }

                // This key is registered only
                // for ENTRY_CREATE events,
                // but an OVERFLOW event can
                // occur regardless if events
                // are lost or discarded.
                if (OVERFLOW == kind) {
                    continue;
                }

                // The filename is the
                // context of the event.
                produceResourceEvent(watchService, kind, directory.resolve((Path) event.context()));


                // Reset the key -- this step is critical if you want to
                // receive further watch events.  If the key is no longer valid,
                // the directory is inaccessible so exit the loop.
                resetKey(watchKey);
            }
        }
    }

    @Override
    public void run() {
        while (running && !currentThread().isInterrupted()) {
            for (final Iterator<WatchService> it = watchServices.values().iterator(); it.hasNext(); ) {
                final WatchService watchService = it.next();
                try {
                    processWatchKey(watchService, watchService.poll());
                } catch (final ClosedWatchServiceException e) {
                    // Remove closed watch-service from map
                    it.remove();
                    LOG.debug(e.getMessage(), e);
                }
            }
        }
    }
}
