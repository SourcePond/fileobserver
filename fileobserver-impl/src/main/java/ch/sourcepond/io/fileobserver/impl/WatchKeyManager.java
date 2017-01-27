package ch.sourcepond.io.fileobserver.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.nio.file.StandardWatchEventKinds.*;

/**
 * Created by rolandhauser on 20.01.17.
 */
public class WatchKeyManager {

    private static class WatchServiceException extends RuntimeException {

        public WatchServiceException(final String message, final Throwable cause) {
            super(message, cause);
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(WatchKeyManager.class);
    private final ConcurrentMap<FileSystem, WatchService> watchServices = new ConcurrentHashMap<>();
    private final ConcurrentMap<Path, WatchKey> watchKeys = new ConcurrentHashMap<>();

    private WatchService getWatchService(final Path pDirectory) throws IOException {
        try {
            return watchServices.computeIfAbsent(pDirectory.getFileSystem(),
                    fs -> newWatchService(fs));
        } catch (final WatchServiceException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    private WatchService newWatchService(final FileSystem pFs) {
        try {
            return pFs.newWatchService();
        } catch (final IOException e) {
            throw new WatchServiceException(e.getMessage(), e);
        }
    }

    Collection<WatchService> getWatchServices() {
        return watchServices.values();
    }

    void cancelWatchKey(final Path pDirectory) {
        final WatchKey watchKey = watchKeys.remove(pDirectory);
        if (null != watchKey) {
            watchKey.cancel();
        } else if (LOG.isWarnEnabled()) {
            LOG.warn(String.format("No watch-key was registered for path %s", pDirectory));
        }
    }

    void openWatchKey(final Path pDirectory) throws IOException {
        try {
            watchKeys.computeIfAbsent(pDirectory, d -> registerWatchKey(d));
        } catch (final WatchServiceException e) {
            throw new IOException(e.getMessage(), e);
        } catch (final ClosedFileSystemException e) {
            watchServices.remove(pDirectory.getFileSystem());
            throw e;
        }
    }

    private WatchKey registerWatchKey(final Path pDirectory) {
        try {
            return pDirectory.register(getWatchService(pDirectory), ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY, OVERFLOW);
        } catch (final IOException e) {
            throw new WatchServiceException(e.getMessage(), e);
        }
    }
}
