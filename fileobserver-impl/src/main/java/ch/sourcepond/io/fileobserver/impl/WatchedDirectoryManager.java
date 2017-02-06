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
 * Created by rolandhauser on 20.01.17.
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
        try {
            enable(pWatchedDirectory.getKey(), pWatchedDirectory.getDirectory());
        } catch (final IOException e) {
            LOG.warn(e.getMessage(), e);
        }
    }

    void unbind(final WatchedDirectory pWatchedDirectory) {
        disable(pWatchedDirectory.getKey());
    }

    private void enable(final Enum<?> pKey, final Path pDirectory) throws IOException {
        requireNonNull(pKey, "Key is null");
        requireNonNull(pDirectory, "Directory is null");

        if (!isDirectory(pDirectory)) {
            throw new IllegalArgumentException(format("[%s]: %s is not a directory!", pKey, pDirectory));
        }

        // Put the directory to the registered paths; if the returned value is null, it's a new key.
        final Path previous = keyToPaths.put(pKey, pDirectory);
        Collection<Enum<?>> keys = pathToKeys.computeIfAbsent(pDirectory, d -> new CopyOnWriteArraySet<>());

        // If the key is newly added, open a watch-service for the directory
        if (keys.add(pKey) && keys.size() == 1) {
            directories.addRoot(pDirectory);
        }

        // The previous directory is not null. This means, that we watch a new target
        // directory, and therefore, need to clean-up.
        disable(pKey, previous);
    }

    private void disable(final Enum<?> pKey) {
        requireNonNull(pKey, "Key is null");
        disable(pKey, keyToPaths.remove(pKey));
    }

    private void disable(final Enum<?> pKey, final Path pToBeDisabled) {
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
