package ch.sourcepond.io.fileobserver.impl;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.WatchService;

/**
 * Created by rolandhauser on 30.01.17.
 */
class WatchKeysFactory {
    private final ResourceEventProducer producer;

    WatchKeysFactory(final ResourceEventProducer pProducer) {
        producer = pProducer;
    }

    WatchKeys createKeys(final FileSystem pFs) {
        try {
            final WatchService watchService = pFs.newWatchService();
            return new WatchKeys(watchService, producer);
        } catch (final IOException e) {
            throw new WatchServiceException(e);
        }
    }
}
