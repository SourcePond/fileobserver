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
import static java.nio.file.Files.walkFileTree;
import static java.nio.file.StandardWatchEventKinds.*;
import static org.slf4j.LoggerFactory.getLogger;

/**
 *
 */
class Registrar implements Closeable, Iterable<FsDirectory> {
    private static final Logger LOG = getLogger(Registrar.class);
    private final ConcurrentMap<Path, FsDirectory> children = new ConcurrentHashMap<>();
    private final WatchService watchService;

    Registrar(final WatchService pWatchService) {
        watchService = pWatchService;
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

    void directoryCreated(final Path pDirectory, final ObserverHandler pHandler) throws IOException {
        walkFileTree(pDirectory, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                pHandler.modified(pDirectory.relativize(file).toString(), file);
                return CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
                try {
                    children.computeIfAbsent(dir,
                            p -> new FsDirectory(children.get(dir.getParent()), register(dir)));
                } catch (final WatchServiceException e) {
                    throw new IOException(e.getMessage(), e);
                }
                return CONTINUE;
            }
        });
    }

    public FsDirectory remove(final Path pDirectory) {
        return children.remove(pDirectory);
    }

    public Collection<Map.Entry<Path, FsDirectory>> entrySet() {
        return children.entrySet();
    }

    public boolean isEmpty() {
        return children.isEmpty();
    }

    public FsDirectory get(final Path parent) {
        return children.get(parent);
    }

    @Override
    public void close() throws IOException {
        try {
            watchService.close();
        } finally {
            children.clear();
        }
    }

    @Override
    public Iterator<FsDirectory> iterator() {
        return children.values().iterator();
    }

    public WatchKey poll() {
        return watchService.poll();
    }
}
