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

import ch.sourcepond.io.fileobserver.api.ResourceObserver;
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
class Directories implements Closeable {
    private static final Logger LOG = getLogger(Directories.class);
    private final ConcurrentMap<FileSystem, FsDirectories> children = new ConcurrentHashMap<>();
    private final ConcurrentMap<ResourceObserver, ObserverHandler> observers = new ConcurrentHashMap<>();
    private final ObserverHandlerFactory observerHandlerFactory;
    private final FsDirectoriesFactory fsDirectoriesFactory;

    public Directories(final ObserverHandlerFactory pObserverHandlerFactory,
                       final FsDirectoriesFactory pFsDirectories) {
        observerHandlerFactory = pObserverHandlerFactory;
        fsDirectoriesFactory = pFsDirectories;
    }

    void addObserver(final ResourceObserver pObserver) {
        final ObserverHandler handler = observerHandlerFactory.newHander(pObserver);
        if (null == observers.putIfAbsent(pObserver, handler)) {
            for (final FsDirectories fsdirs : children.values()) {
                fsdirs.initialyInformHandler(handler);
            }
        }
    }

    void removeObserver(final ResourceObserver pObserver) {
        observers.remove(pObserver);
    }

    private WatchService newWatchService(final FileSystem pFs) {
        try {
            return pFs.newWatchService();
        } catch (final IOException e) {
            throw new WatchServiceException(e);
        }
    }

    void addRoot(final Path pDirectory) throws IOException {
        if (!isDirectory(pDirectory)) {
            throw new IllegalArgumentException(format("%s is not a directory!", pDirectory));
        }
        try {
            children.computeIfAbsent(pDirectory.getFileSystem(), fs ->
                    fsDirectoriesFactory.newDirectories(
                            newWatchService(fs))).directoryCreated(pDirectory, observers.values());
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

    private String toId(final Path pPath) {
        return getFsDirectories(pPath).getDirectory(pPath).relativize(pPath);
    }

    void pathCreated(final Path pPath) {
        if (isDirectory(pPath)) {
            try {
                getFsDirectories(pPath).directoryCreated(pPath, observers.values());
            } catch (final IOException e) {
                LOG.warn(e.getMessage(), e);
            }
        } else {
            observers.values().forEach(h -> h.modified(toId(pPath), pPath));
        }
    }

    void pathDeleted(final Path pPath) {
        final FsDirectories fsdirs = getFsDirectories(pPath);
        final String id = toId(pPath);
        if (fsdirs.directoryDeleted(pPath)) {
            children.remove(pPath.getFileSystem());
        }
        observers.values().forEach(h -> h.deleted(id));
    }

    @Override
    public void close() {
        for (final FsDirectories fsdirs : children.values()) {
            fsdirs.close();
        }
        children.clear();
    }

    void processFsEvents(final WatchKeyProcessor pProcessor) {
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
