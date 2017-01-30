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

import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;
import static java.lang.Thread.currentThread;
import static java.nio.file.Files.isDirectory;
import static java.nio.file.StandardWatchEventKinds.*;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * <p>This class wraps a {@link WatchService} and the associated event consumer thread.
 * It holds the watch-keys of exactly one {@link java.nio.file.FileSystem} instance. Instances of
 * this class are used as part of {@link WatchServices}.
 * <p>
 * This class is <em>not</em> thread-safe and must be synchronized externally.</p>
 */
class WatchKeys implements Runnable {
    private static final Logger LOG = getLogger(WatchKeys.class);
    private final Map<Path, WatchKey> watchKeys = new HashMap<>();
    private final WatchService watchService;
    private final ResourceEventProducer resourceEventProducer;

    WatchKeys(final WatchService pWatchService,
              final ResourceEventProducer pResourceEventProducer) {
        watchService = pWatchService;
        resourceEventProducer = pResourceEventProducer;
    }

    /**
     * Interrupts the worker thread which takes events from the managed
     * watch-service. Then, it delegates to the {@link WatchService#close()} method
     * of the managed watch-service.
     */
    public void shutdown() {
        currentThread().interrupt();
        try {
            watchService.close();
        } catch (final IOException e) {
            LOG.warn(e.getMessage(), e);
        }
    }

    /**
     * @return {@code true} if no more watch-keys are registered, {@code false}
     */
    boolean isEmpty() {
        synchronized (watchKeys) {
            return watchKeys.isEmpty();
        }
    }

    private boolean doCancelWatchKey(final Path pDirectory) {
        final WatchKey watchKey;
        synchronized (watchKeys) {
            watchKey = watchKeys.remove(pDirectory);
        }
        if (null != watchKey) {
            watchKey.cancel();
            return true;
        } else if (LOG.isWarnEnabled()) {
            LOG.warn(format("No watch-key was registered for path %s", pDirectory));
        }
        return false;
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

    /**
     * Registers the directory specified with the watch-service of this object. The
     * path must have the same {@link java.nio.file.FileSystem} as the managed watch-service.
     *
     * @param pDirectory Directory, must not be {@code null}
     * @throws IOException Thrown, if something went wrong during registration.
     */
    void openWatchKey(final Path pDirectory) throws IOException {
        synchronized (watchKeys) {
            if (!watchKeys.containsKey(pDirectory)) {
                watchKeys.putAll(new WatchServiceInstaller(watchService, pDirectory).registerDirectories());
            }
        }
    }

    @Override
    public void run() {
        try {
            while (!currentThread().isInterrupted()) {
                final WatchKey watchKey = watchService.take();
                final Path directory = (Path) watchKey.watchable();
                try {
                    for (final WatchEvent<?> event : watchKey.pollEvents()) {
                        final WatchEvent.Kind<?> kind = event.kind();
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
                        final Path child = directory.resolve((Path) event.context());
                        if (isDirectory(child) && ENTRY_CREATE == kind) {
                            final WatchServiceInstaller installer = new WatchServiceInstaller(watchService, child);
                            try {
                                installer.registerDirectories();
                            } catch (IOException e) {
                                LOG.warn(e.getMessage(), e);
                            }
                        } else if (ENTRY_CREATE == kind || ENTRY_MODIFY == kind) {
                            resourceEventProducer.fileModify(child);
                        } else if (ENTRY_DELETE == kind && !doCancelWatchKey(child)) {
                            resourceEventProducer.fileDelete(child);
                        }
                    }
                } finally {
                    // Reset the key -- this step is critical if you want to
                    // receive further watch events.  If the key is no longer valid,
                    // the directory is inaccessible so exit the loop.
                    if (!watchKey.reset()) {
                        break;
                    }
                }

            }
        } catch (final ClosedWatchServiceException | InterruptedException e) {
            currentThread().interrupt();
            LOG.warn(e.getMessage(), e);
        }
    }
}
