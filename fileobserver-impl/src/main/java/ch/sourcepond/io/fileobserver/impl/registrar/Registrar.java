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
package ch.sourcepond.io.fileobserver.impl.registrar;

import ch.sourcepond.io.fileobserver.api.FileObserver;
import ch.sourcepond.io.fileobserver.impl.ExecutorServices;
import ch.sourcepond.io.fileobserver.impl.directory.FsBaseDirectory;
import ch.sourcepond.io.fileobserver.impl.directory.FsDirectoryFactory;
import ch.sourcepond.io.fileobserver.impl.directory.FsRootDirectory;
import org.slf4j.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.lang.String.format;
import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.Files.walkFileTree;
import static java.nio.file.StandardWatchEventKinds.*;
import static org.slf4j.LoggerFactory.getLogger;

/**
 *
 */
public class Registrar implements Closeable {
    private static final Logger LOG = getLogger(Registrar.class);
    private final ConcurrentMap<Path, FsBaseDirectory> children = new ConcurrentHashMap<>();
    private final ExecutorServices executorServices;
    private final FsDirectoryFactory directoryFactory;
    private final WatchService watchService;

    Registrar(final ExecutorServices pExecutorServices, final FsDirectoryFactory pDirectoryFactory, final WatchService pWatchService) {
        executorServices = pExecutorServices;
        directoryFactory = pDirectoryFactory;
        watchService = pWatchService;
    }

    private WatchKey register(final Path pDirectory) {
        try {
            return pDirectory.register(watchService, new WatchEvent.Kind[]{
                    ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY});
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            if (LOG.isDebugEnabled()) {
                LOG.debug(format("Added Directory %s", pDirectory));
            }
        }
    }

    public void initiallyInformHandler(final Collection<FileObserver> pObservers) {
        children.values().forEach(d -> d.forceInformAboutAllDirectChildFiles(pObservers));
    }

    public void rootAdded(final Object pWatchedDirectoryKey, final Path pDirectory, final Collection<FileObserver> pObservers) {
        if (!children.containsKey(pDirectory)) {
            final FsRootDirectory rootDir = directoryFactory.newRoot(pWatchedDirectoryKey);
            if (null == children.putIfAbsent(pDirectory, rootDir)) {
                rootDir.setWatchKey(register(pDirectory));
                executorServices.getDirectoryWalkerExecutor().execute(
                        () -> directoryCreated(pDirectory, pObservers));
            }
        }
    }

    public void directoryCreated(final Path pDirectory, final Collection<FileObserver> pObserver) {
        try {
            walkFileTree(pDirectory, new SimpleFileVisitor<Path>() {

                @Override
                public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                    children.get(file.getParent()).forceInformObservers(pObserver, file);
                    return CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
                    children.computeIfAbsent(dir,
                            p -> directoryFactory.newBranch(children.get(dir.getParent()), register(dir)));
                    return CONTINUE;
                }
            });
        } catch (final UncheckedIOException // Can happen when watch-service registration fails, see register
                | IOException e) {
            LOG.warn(e.getMessage(), e);
        }
    }

    public boolean directoryDeleted(final Path pDirectory) {
        final FsBaseDirectory dir = children.remove(pDirectory);
        if (null != dir) {
            dir.cancelKey();
            for (final Iterator<Map.Entry<Path, FsBaseDirectory>> it = children.entrySet().iterator(); it.hasNext(); ) {
                final Map.Entry<Path, FsBaseDirectory> entry = it.next();
                if (entry.getKey().startsWith(pDirectory)) {
                    entry.getValue().cancelKey();
                    it.remove();
                }
            }
        }
        return children.isEmpty();
    }

    public FsBaseDirectory getDirectory(final Path parent) {
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

    public WatchKey poll() {
        return watchService.poll();
    }
}
