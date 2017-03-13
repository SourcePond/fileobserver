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
import java.nio.file.*;

import static java.lang.String.format;
import static java.nio.file.StandardWatchEventKinds.*;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Wraps the {@link WatchService} specified.
 */
public class WatchServiceWrapper implements Closeable {
    private static final Logger LOG = getLogger(WatchServiceWrapper.class);
    private final FileSystem fs;
    private final WatchService watchService;

    public WatchServiceWrapper(final FileSystem pFs) throws IOException {
        fs = pFs;
        watchService = pFs.newWatchService();
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
     * See {@link WatchService#take}.
     *
     * @return
     */
    public WatchKey take() throws InterruptedException {
        return watchService.take();
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
        try {
            final WatchKey key = pDirectory.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
            if (LOG.isDebugEnabled()) {
                LOG.debug(format("Added Directory %s", pDirectory));
            }
            return key;
        } catch (final ClosedWatchServiceException e) {
            throw new IOException(format("Closed WatchService! Registration failed for %s", pDirectory), e);
        }
    }

    @Override
    public String toString() {
        return fs.provider().getScheme();
    }
}
