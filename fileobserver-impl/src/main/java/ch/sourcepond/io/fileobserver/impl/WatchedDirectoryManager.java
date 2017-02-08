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

import ch.sourcepond.io.fileobserver.api.WatchedDirectory;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;

import static java.lang.String.format;
import static java.nio.file.Files.isDirectory;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 *
 */
public class WatchedDirectoryManager {
    private static final Logger LOG = getLogger(WatchedDirectoryManager.class);
    private final ConcurrentMap<Enum<?>, Path> keyToPaths = new ConcurrentHashMap<>();
    private final ConcurrentMap<Path, Collection<Enum<?>>> pathToKeys = new ConcurrentHashMap<>();
    private final Directories directories;

    public WatchedDirectoryManager(final Directories pDirectories) {
        directories = pDirectories;
    }

    public void bind(final WatchedDirectory pWatchedDirectory) {
        requireNonNull(pWatchedDirectory, "Watched directory is null");
        try {
            final Enum<?> key = pWatchedDirectory.getKey();
            final Path directory = pWatchedDirectory.getDirectory();
            requireNonNull(key, "Key is null");
            requireNonNull(directory, "Directory is null");

            if (!isDirectory(directory)) {
                throw new IllegalArgumentException(format("[%s]: %s is not a directory!", key, directory));
            }

            // Put the directory to the registered paths; if the returned value is null, it's a new key.
            final Path previous = keyToPaths.put(key, directory);

            if (!directory.equals(previous)) {
                final Collection<Enum<?>> keys = pathToKeys.computeIfAbsent(directory, d -> new CopyOnWriteArraySet<>());

                // If the key is newly added, open a watch-service for the directory
                if (keys.add(key) && keys.size() == 1) {
                    directories.addRoot(directory);
                }

                // The previous directory is not null. This means, that we watch a new target
                // directory, and therefore, need to clean-up.
                disableIfNecessary(key, previous);
            }
        } catch (final IOException e) {
            LOG.warn(e.getMessage(), e);
        }
    }

    void unbind(final WatchedDirectory pWatchedDirectory) {
        requireNonNull(pWatchedDirectory, "Watched directory is null");
        final Enum<?> key = pWatchedDirectory.getKey();
        requireNonNull(key, "Key is null");
        disableIfNecessary(key, keyToPaths.remove(key));
    }

    private void disableIfNecessary(final Enum<?> pKey, final Path pToBeDisabled) {
        if (null != pToBeDisabled) {
            final Collection<Enum<?>> keys = pathToKeys.getOrDefault(pToBeDisabled, emptyList());

            // If no more keys are registered for the previous directory, its watch-key
            // needs to be cancelled.
            if (keys.remove(pKey) && keys.isEmpty()) {
                directories.removeRoot(pToBeDisabled);
                pathToKeys.remove(pKey);
            }
        }
    }
}
