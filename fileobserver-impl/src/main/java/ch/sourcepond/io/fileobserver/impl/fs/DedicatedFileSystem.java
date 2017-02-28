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
package ch.sourcepond.io.fileobserver.impl.fs;

import ch.sourcepond.io.fileobserver.api.FileObserver;
import ch.sourcepond.io.fileobserver.impl.ExecutorServices;
import ch.sourcepond.io.fileobserver.impl.directory.Directory;
import ch.sourcepond.io.fileobserver.impl.directory.DirectoryFactory;
import ch.sourcepond.io.fileobserver.impl.directory.RootDirectory;
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
public class DedicatedFileSystem implements Closeable {
    private static final Logger LOG = getLogger(DedicatedFileSystem.class);
    private final ConcurrentMap<Path, Directory> dirs = new ConcurrentHashMap<>();
    private final ExecutorServices executorServices;
    private final DirectoryFactory directoryFactory;
    private final WatchService watchService;

    DedicatedFileSystem(final ExecutorServices pExecutorServices, final DirectoryFactory pDirectoryFactory, final WatchService pWatchService) {
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

    public void initiallyInformHandler(final FileObserver pObserver) {
        dirs.values().forEach(d -> d.forceInformAboutAllDirectChildFiles(pObserver));
    }

    /**
     *
     *
     * @param pDirectoryKey
     * @param pDirectory
     * @param pObservers
     */
    public void rootAdded(final Object pDirectoryKey, final Path pDirectory, final Collection<FileObserver> pObservers) {
        // Check if there is already a directory available for the path specified.
        Directory dir = dirs.get(pDirectory);

        if (dir == null) {
            // If no directory is registered for the path specified, create a new root-directory.
            dir = directoryFactory.newRoot();

            if (null == dirs.putIfAbsent(pDirectory, dir)) {
                // Register the path with the watch-service and set the watch-key on the
                // newly create root-directory
                ((RootDirectory) dir).setWatchKey(register(pDirectory));
                dirs.put(pDirectory, dir);

                // Asynchronously register all sub-directories with the watch-service, and,
                // inform the registered FileObservers
                executorServices.getDirectoryWalkerExecutor().execute(
                        () -> directoryCreated(pDirectory, pObservers));
            }
        }

        // In any case, add the directory key to the directory
        dir.addDirectoryKey(pDirectoryKey);
    }

    public void directoryCreated(final Path pDirectory, final Collection<FileObserver> pObserver) {
        try {
            walkFileTree(pDirectory, new SimpleFileVisitor<Path>() {

                @Override
                public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                    dirs.get(file.getParent()).forceInformObservers(pObserver, file);
                    return CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
                    dirs.computeIfAbsent(dir,
                            p -> directoryFactory.newBranch(dirs.get(dir.getParent()), register(dir)));
                    return CONTINUE;
                }
            });
        } catch (final UncheckedIOException // Can happen when watch-service registration fails, see register
                | IOException e) {
            LOG.warn(e.getMessage(), e);
        }
    }

    public boolean directoryDeleted(final Path pDirectory) {
        final Directory dir = dirs.remove(pDirectory);
        if (null != dir) {
            dir.cancelKey();
            for (final Iterator<Map.Entry<Path, Directory>> it = dirs.entrySet().iterator(); it.hasNext(); ) {
                final Map.Entry<Path, Directory> entry = it.next();
                if (entry.getKey().startsWith(pDirectory)) {
                    entry.getValue().cancelKey();
                    it.remove();
                }
            }
        }
        return dirs.isEmpty();
    }

    public Directory getParentDirectory(final Path pFile) {
        final Directory dir = dirs.get(pFile.getParent());
        if (null == dir) {
            throw new NullPointerException(format("No parent directory found for file %s", pFile));
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
            dirs.clear();
        }
    }

    public WatchKey poll() {
        return watchService.poll();
    }
}
