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

import ch.sourcepond.io.fileobserver.api.FileKey;
import ch.sourcepond.io.fileobserver.api.FileObserver;
import ch.sourcepond.io.fileobserver.impl.ExecutorServices;
import ch.sourcepond.io.fileobserver.impl.directory.Directory;
import ch.sourcepond.io.fileobserver.impl.directory.DirectoryFactory;
import ch.sourcepond.io.fileobserver.spi.WatchedDirectory;
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
import static java.util.Objects.requireNonNull;
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

    /**
     * Registers the path specified with the {@link WatchService} held by this object.
     * If the path specified is not a directory, an {@link UncheckedIOException} will be caused to be thrown.
     *
     * @param pDirectory Directory to be watched, must not be {@code null}
     * @return A key representing the registration of this object with the watch service
     * @throws IOException Thrown, if the registration of the directory specified with the watch service failed.
     */
    private WatchKey register(final Path pDirectory) throws IOException {
        final WatchKey key = pDirectory.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
        if (LOG.isDebugEnabled()) {
            LOG.debug(format("Added Directory %s", pDirectory));
        }
        return key;
    }

    /**
     * <p>Iterates through all registered directories and passes all their files to the
     * {@link FileObserver#modified(FileKey, Path)} of the observer specified. This is necessary for newly registered
     * observers who need to know about all watched files. See {@link #registerRootDirectory(WatchedDirectory, Collection)} and
     * {@link #directoryCreated(Path, Collection)} to get an idea how directories are registered with this object.
     * <p>Note: it's guaranteed that the {@link Path} instances passed
     * to the observer are regular files (not directories).
     *
     * @param pObserver Oberserver to be informed, must not be {@code null}.
     */
    public void forceInform(final FileObserver pObserver) {
        dirs.values().forEach(d -> d.forceInform(pObserver));
    }

    /**
     * @param pWatchedDirectory
     * @param pObservers
     */
    // Note: Despite a ConcurrenMap is used it's necessary to synchronize this method.
    // The reason is because all sub-directories need to registered before another WatchedDirectory
    // can be registered through this method.
    public synchronized void registerRootDirectory(final WatchedDirectory pWatchedDirectory,
                                      final Collection<FileObserver> pObservers)
            throws IOException {
        final Object key = requireNonNull(pWatchedDirectory.getKey(), "Key is null");

        // It's already checked that the directory is not null
        final Path directory = pWatchedDirectory.getDirectory();

        Directory dir = dirs.get(directory);
        if (dir == null) {
            // If no directory is registered for the path specified, create a new root-directory.
            // Register the path with the watch-service and use the returned watch-key to create the root directory.
            dir = directoryFactory.newRoot(register(directory));
            dirs.put(directory, dir);

            // Asynchronously register all sub-directories with the watch-service, and,
            // inform the registered FileObservers
            executorServices.getDirectoryWalkerExecutor().execute(
                    () -> directoryCreated(directory, pObservers));
        }

        // In any case, add the directory key to the directory
        dir.addDirectoryKey(key);
    }

    public void unregisterRootDirectory(final WatchedDirectory pWatchedDirectory,
                                        final Collection<FileObserver> pObservers) {
        final Object key = requireNonNull(pWatchedDirectory.getKey(), "Key is null");
        final Path directory = requireNonNull(pWatchedDirectory.getDirectory(), "Directory is null");

        final Directory dir = dirs.get(directory);
        dir.removeDirectoryKey(key);

        // Discard the root-directory; after this the directory is not watched anymore.
        directoryDiscarded(pObservers, directory);

    }

    /**
     * Registers the directory specified and all its sub-directories with the watch-service held by this object.
     * Additionally, it passes any detected file to {@link FileObserver#modified(FileKey, Path)} to the observers
     * specified.
     *
     * @param pDirectory Newly created directory, must not be {@code null}
     * @param pObservers Observers to be informed about detected files, must not be {@code null}
     */
    void directoryCreated(final Path pDirectory, final Collection<FileObserver> pObservers) {
        try {
            walkFileTree(pDirectory, new SimpleFileVisitor<Path>() {

                @Override
                public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                    // It's important here to only trigger the observers if the file has changed.
                    // This is most certainly the case, but, there is an exception: because we already
                    // registered the parent directory of the file with the watch-service there's a small
                    // chance that the file had already been modified before we got here.
                    dirs.get(file.getParent()).informIfChanged(pObservers, file);
                    return CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
                    try {
                        dirs.computeIfAbsent(dir,
                                p -> {
                                    try {
                                        return directoryFactory.newBranch(dirs.get(dir.getParent()), register(dir));
                                    } catch (final IOException e) {
                                        throw new UncheckedIOException(e.getMessage(), e);
                                    }
                                });
                        return CONTINUE;
                    } catch (final UncheckedIOException e) {
                        throw new IOException(e.getMessage(), e);
                    }
                }
            });
        } catch (final IOException e) {
            LOG.warn(e.getMessage(), e);
        }
    }

    public boolean directoryDiscarded(final Collection<FileObserver> pObservers, final Path pDirectory) {
        final Directory dir = dirs.remove(pDirectory);
        final boolean wasDirectory = dir != null;
        if (wasDirectory) {
            dir.cancelKey();
            for (final Iterator<Map.Entry<Path, Directory>> it = dirs.entrySet().iterator(); it.hasNext(); ) {
                final Map.Entry<Path, Directory> entry = it.next();
                if (entry.getKey().startsWith(pDirectory)) {
                    entry.getValue().cancelKey();
                    it.remove();
                }
            }
            dir.informDiscard(pObservers, pDirectory);
        }
        return wasDirectory;
    }

    public Directory getDirectory(final Path pPath) {
        return dirs.get(pPath);
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
