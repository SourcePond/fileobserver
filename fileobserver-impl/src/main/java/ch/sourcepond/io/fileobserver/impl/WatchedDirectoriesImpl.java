package ch.sourcepond.io.fileobserver.impl;

import ch.sourcepond.io.fileobserver.api.WatchedDirectories;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.util.*;

import static java.nio.file.Files.isDirectory;
import static java.util.Objects.requireNonNull;

/**
 * Created by rolandhauser on 19.01.17.
 */
public class WatchedDirectoriesImpl implements WatchedDirectories {
    private final Map<Enum<?>, Path> keyToPath = new HashMap<>();
    private final Map<Path, Collection<Enum<?>>> pathToKeys = new HashMap<>();
    private final WatchKeyManager keyManager;
    private final ResourceEventProducer producer;

    public WatchedDirectoriesImpl(final WatchKeyManager pKeyManager,
                                  final ResourceEventProducer pProducer) {
        keyManager = pKeyManager;
        producer = pProducer;
    }

    public Collection<Enum<?>> getKeys(final Path pPath) {
        Collection<Enum<?>> keys = pathToKeys.get(pPath);
        if (null == keys) {
            keys = new HashSet<>();
            pathToKeys.put(pPath, keys);
        }
        return keys;
    }

    public Path getDirectory(final Enum<?> pKey) throws NoSuchFileException {
        final Path directory = keyToPath.get(pKey);
        if (null == directory) {
            throw new NoSuchFileException("unknown");
        }
        return directory;
    }

    @Override
    public void enable(final Enum<?> pKey, final Path pDirectory) throws IOException {
        requireNonNull(pKey, "Key specified is null");
        requireNonNull(pDirectory, "Directory specified is null");
        if (!isDirectory(pDirectory)) {
            throw new NotDirectoryException(pDirectory.toString());
        }

        // Put the directory to the registered paths; if the returned value is null, it's a new key.
        final Path previousDirectory = keyToPath.put(pKey, pDirectory);

        // Add the key to the key-set of the new directory specified.
        final Collection<Enum<?>> directoryKeys = getKeys(pDirectory);

        // If the key is newly added, open a watch-service for the directory
        if (directoryKeys.add(pKey) && directoryKeys.size() == 1) {
            keyManager.openWatchKey(pDirectory);
        }

        if (null != previousDirectory) {
            // Get the keys of the previous directory; we need to remove the current
            // key from it.
            final Collection<Enum<?>> keysOfPreviousDirectory = getKeys(previousDirectory);

            // If no more keys are registered for the previous directory, its watch-service needs to be closed.
            if (keysOfPreviousDirectory.remove(pKey) && keysOfPreviousDirectory.isEmpty()) {
                keyManager.cancelWatchKey(previousDirectory);

                // Clean-up
                pathToKeys.remove(previousDirectory);
            }

        }
    }

    @Override
    public void disable(final Enum<?> pKey) {

    }
}
