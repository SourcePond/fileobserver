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

import org.slf4j.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

import static java.lang.String.format;
import static java.nio.file.StandardWatchEventKinds.*;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Wraps the {@link WatchService} specified.
 */
class WatchServiceWrapper implements Closeable {
    private static final Logger LOG = getLogger(WatchServiceWrapper.class);
    private final WatchService watchService;

    WatchServiceWrapper(final WatchService pWatchService) {
        watchService = pWatchService;
    }

    @Override
    public void close() {
        try {
            watchService.close();
        } catch (final IOException e) {
            LOG.warn(e.getMessage(), e);
        }
    }

    /**
     * See {@link WatchService#poll}.
     *
     * @return
     */
    public WatchKey poll() {
        return watchService.poll();
    }

    /**
     * Registers the path specified with the {@link java.nio.file.WatchService} held by this object.
     * If the path specified is not a directory, an {@link UncheckedIOException} will be caused to be thrown.
     *
     * @param pDirectory Directory to be watched, must not be {@code null}
     * @return A key representing the registration of this object with the watch service
     * @throws IOException Thrown, if the registration of the directory specified with the watch service failed.
     */
    public WatchKey register(final Path pDirectory) throws IOException {
        final WatchKey key = pDirectory.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
        if (LOG.isDebugEnabled()) {
            LOG.debug(format("Added Directory %s", pDirectory));
        }
        return key;
    }
}
