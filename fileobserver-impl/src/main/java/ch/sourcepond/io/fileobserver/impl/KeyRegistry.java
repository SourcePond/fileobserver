package ch.sourcepond.io.fileobserver.impl;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

import static java.nio.file.Files.walkFileTree;

/**
 * Created by rolandhauser on 20.01.17.
 */
public class KeyRegistry {
    private final ConcurrentMap<Enum<?>, Path> keyToPath = new ConcurrentHashMap<>();
    private final WatchKeys manager;
    private final ResourceEventProducer producer;

    public KeyRegistry(final WatchKeys pManager, final ResourceEventProducer pProducer) {
        manager = pManager;
        producer = pProducer;
    }

    public void put(final Enum<?> pKey, final Path pDirectory) throws IOException {
        // Put the directory to the registered paths; if the returned value is null, it's a new key.
        final Path previousDirectory = keyToPath.put(pKey, pDirectory);

        // Add the key to the key-set of the new directory specified.
        final Collection<Enum<?>> directoryKeys = getKeys(pDirectory);

        // If the key is newly added, open a watch-service for the directory
        if (directoryKeys.add(pKey) && directoryKeys.size() == 1) {
            manager.openWatchKey(pDirectory);
        }

        // The previous directory is not null. This means, that we watch a new target
        // directory
        if (null != previousDirectory) {
            // Get the keys of the previous directory; we need to remove the current
            // key from it.
            final Collection<Enum<?>> keysOfPreviousDirectory = getKeys(previousDirectory);

            // If no more keys are registered for the previous directory, its watch-key
            // needs to be cancelled.
            if (keysOfPreviousDirectory.remove(pKey) && keysOfPreviousDirectory.isEmpty()) {
                manager.cancelWatchKey(previousDirectory);
            }

            // We should update all file-checksums, to detect
            // new or changed files.
            walkFileTree(pDirectory, new FileCollector(pDirectory, f -> producer.fileModify(f)));
        }
    }

    private Collection<Enum<?>> getKeys(final Path pDirectory) {
        final Collection<Enum<?>> keys = new LinkedList<>();
        keyToPath.entrySet().forEach(e -> {
            if (pDirectory.equals(e.getValue())) {
                keys.add(e.getKey());
            }
        });
        return keys;
    }

    private void walkDirectory(final Path pDirectory) throws IOException {
        walkFileTree(pDirectory, new FileCollector(pDirectory, f -> producer.fileModify(f)));
    }

    void walkFiles(final Enum<?>[] keys, final Consumer<Path> pConsumer) throws IOException {
        final Collection<Path> rootDirectories = new HashSet<>();
        for (final Enum<?> key : keys) {
            rootDirectories.add(keyToPath.get(key));
        }
        for (final Path directory : rootDirectories) {
            walkDirectory(directory);
        }
    }
}
