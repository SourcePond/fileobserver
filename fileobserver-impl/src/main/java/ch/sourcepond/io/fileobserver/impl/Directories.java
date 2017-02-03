package ch.sourcepond.io.fileobserver.impl;

import org.slf4j.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.*;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.lang.String.format;
import static java.nio.file.Files.isDirectory;
import static org.slf4j.LoggerFactory.getLogger;

/**
 *
 */
class Directories implements Closeable, Iterable<FsDirectories> {
    private static final Logger LOG = getLogger(FsDirectories.class);
    private final ConcurrentMap<FileSystem, FsDirectories> children = new ConcurrentHashMap<>();
    private final ResourceEventProducer resourceEventProducer;

    Directories(final ResourceEventProducer pResourceEventProducer) {
        resourceEventProducer = pResourceEventProducer;
    }

    @Override
    public Iterator<FsDirectories> iterator() {
        return children.values().iterator();
    }

    private WatchService newWatchService(final FileSystem pFs) {
        try {
            return pFs.newWatchService();
        } catch (final IOException e) {
            throw new WatchServiceException(e);
        }
    }

    void addRoot(final Path pDirectory) throws IOException {
        try {
            children.computeIfAbsent(pDirectory.getFileSystem(), fs ->
                    new FsDirectories(newWatchService(fs))).directoryCreated(pDirectory);
        } catch (final WatchServiceException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    void removeRoot(final Path pDirectory) {
        final FsDirectories fsdirs = children.get(pDirectory.getFileSystem());
        if (null != fsdirs && fsdirs.directoryDeleted(pDirectory)) {
            children.remove(pDirectory.getFileSystem());
        }
    }

    private FsDirectories getFsDirectories(final Path pPath) {
        final FsDirectories fsdirs = children.get(pPath.getFileSystem());
        if (null == fsdirs) {
            throw new IllegalStateException(format("No appropriate root-directory found for %s", pPath));
        }
        return fsdirs;
    }

    void fileModified(final Path pFile) {
        getFsDirectories(pFile).fileModified(resourceEventProducer, pFile);
    }

    void pathCreated(final Path pPath)  {
        if (isDirectory(pPath)) {
            try {
                getFsDirectories(pPath).directoryCreated(pPath);
            } catch (final IOException e) {
                LOG.warn(e.getMessage(), e);
            }
        } else {
            fileModified(pPath);
        }
    }

    void removeInvalidDirectory(final Path pInvalid) {
        getFsDirectories(pInvalid).directoryDeleted(pInvalid);
    }

    void pathDeleted(final Path pPath)  {
        final FsDirectories fsdirs = getFsDirectories(pPath);
        if (!fsdirs.directoryDeleted(pPath)) {
            getFsDirectories(pPath).fileDeleted(resourceEventProducer, pPath);
        }
        if (fsdirs.isEmpty()) {
            children.remove(pPath.getFileSystem());
        }
    }

    @Override
    public void close() {
        for (final FsDirectories fsdirs : children.values()) {
            fsdirs.close();
        }
        children.clear();
    }

    void processFsEvents(WatchKeyProcessor pProcessor) {
        for (final Iterator<FsDirectories> it = children.values().iterator(); it.hasNext(); ) {
            final FsDirectories next = it.next();
            final WatchKey key = next.poll();
            if (null != key) {
                try {
                    pProcessor.processEvent(key);
                } catch (final IOException e) {
                    LOG.warn(e.getMessage(), e);
                } catch (final ClosedWatchServiceException e) {
                    next.close();
                    // Remove closed watch-service from map
                    it.remove();
                    LOG.debug(e.getMessage(), e);
                }
            }
        }
    }
}
