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

import org.slf4j.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.sun.nio.file.SensitivityWatchEventModifier.HIGH;
import static java.lang.String.format;
import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.Files.list;
import static java.nio.file.Files.walkFileTree;
import static java.nio.file.StandardWatchEventKinds.*;
import static org.slf4j.LoggerFactory.getLogger;

/**
 *
 */
class FsDirectories implements Closeable {
    private static final Logger LOG = getLogger(FsDirectories.class);
    private final ConcurrentMap<Path, FsDirectory> children = new ConcurrentHashMap<>();
    private final WatchService watchService;

    FsDirectories(final WatchService pWatchService) {
        watchService = pWatchService;
    }

    void initialyInformHandler(final ObserverHandler pHandler) {
        for (final FsDirectory fsdir : children.values()) {
            try {
                list(fsdir.getPath()).forEach(f -> {
                    pHandler.modified(fsdir.relativize(f), f);
                });
            } catch (final IOException e) {
                LOG.warn(e.getMessage(), e);
            }
        }
    }

    void directoryCreated(final Path pDirectory, final Collection<ObserverHandler> pHandlers) throws IOException {
        try {
            walkFileTree(pDirectory, new SimpleFileVisitor<Path>() {

                @Override
                public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                    pHandlers.forEach(h -> h.modified(pDirectory.relativize(file).toString(), file));
                    return CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
                    children.computeIfAbsent(dir,
                            p -> new FsDirectory(children.get(dir.getParent()), register(dir)));
                    return CONTINUE;
                }
            });
        } catch (final IOException e) {
            throw new WatchServiceException(e);
        }
    }

    private WatchKey register(final Path pDirectory) throws WatchServiceException {
        try {
            return pDirectory.register(watchService, new WatchEvent.Kind[]{
                    ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY}, HIGH);
        } catch (final IOException e) {
            throw new WatchServiceException(e);
        } finally {
            if (LOG.isDebugEnabled()) {
                LOG.debug(format("Added Directory %s", pDirectory));
            }
        }
    }

    boolean directoryDeleted(final Path pDirectory) {
        final FsDirectory dir = children.remove(pDirectory);
        if (null != dir) {
            dir.cancelKey();
            for (final Iterator<Map.Entry<Path, FsDirectory>> it = children.entrySet().iterator() ; it.hasNext() ; ) {
                final Map.Entry<Path, FsDirectory> entry = it.next();
                if (entry.getKey().startsWith(pDirectory)) {
                    entry.getValue().cancelKey();
                    it.remove();
                }
            }
        }
        return children.isEmpty();
    }

    boolean isEmpty() {
        return children.isEmpty();
    }

    FsDirectory getDirectory(final Path pFile) {
        final FsDirectory dir = children.get(pFile.getParent());
        if (null == dir) {
            throw new NullPointerException(format("No directory object found for file %s", pFile));
        }
        return dir;
    }

    @Override
    public void close() {
        try {
            watchService.close();
        } catch (final IOException e) {
            LOG.warn(e.getMessage(), e);
        } finally {
            children.clear();
        }
    }

    WatchKey poll() {
        return watchService.poll();
    }
}
