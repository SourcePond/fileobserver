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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.lang.String.format;
import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.SKIP_SUBTREE;
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

    DedicatedFileSystem(final ExecutorServices pExecutorServices,
                        final DirectoryFactory pDirectoryFactory,
                        final WatchService pWatchService) {
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
     * Collects the paths between the parent and the child path specified. The returned collection will <em>not</em>
     * contain the parent and child path specified, and, will be ordered from the base path to the farthest child.
     *
     * @param pParent The parent path, must not be {@code null}
     * @param pChild  The child path, must not be {@code null}
     * @return Collection of paths in between excluding the parent and child specified
     */
    private Collection<Path> pathsInBetweenOf(final Path pParent, final Path pChild) {
        if (!pChild.startsWith(pParent)) {
            throw new IllegalArgumentException(format("%s is not a child of %s", pChild, pParent));
        }
        final Deque<Path> hierarchy = new LinkedList<>();
        Path p = pChild.getParent();
        while (!p.equals(pParent)) {
            hierarchy.addFirst(p);
            p = p.getParent();
        }
        return hierarchy;
    }

    /**
     * Collects all existing root directories which are children of the new root directory specified specified.
     *
     * @param pNewRoot New root directory to match, must not be {@code null}
     * @return Collection of directories, never {@code null}.
     */
    private Collection<Directory> collectExistingRoots(final Directory pNewRoot) {
        final Path parentPath = pNewRoot.getPath();
        final Collection<Directory> pathsToRebase = new LinkedList<>();
        dirs.entrySet().forEach(e -> {
            if (e.getKey().startsWith(parentPath) && e.getValue().isRoot()) {
                pathsToRebase.add(e.getValue());
            }
        });
        return pathsToRebase;
    }

    /**
     * Sets the directory specified on all directories whose path is a direct child of
     * {@link Directory#getPath()} of the base directory specified.
     *
     * @param pBaseDirectory Parent directory to set, must not be {@code null}
     */
    private void rebaseDirectSubDirectories(final Directory pBaseDirectory) {
        final Path base = pBaseDirectory.getPath();
        dirs.forEach((k, v) -> {
            if (base.equals(k.getParent())) {
                v.rebase(pBaseDirectory);
            }
        });
    }

    private void rebaseExistingRootDirectories(final Directory pRoot) throws IOException {
        // Iterate over already registered root directories which should be converted to
        // sub-directories (rebased).
        for (final Directory existingRoot : collectExistingRoots(pRoot)) {

            // Create all missing directories between the new root and
            // the existing roots which shall be rebased.
            Directory parent = pRoot;
            for (final Path missingLevel : pathsInBetweenOf(pRoot.getPath(), existingRoot.getPath())) {
                parent = directoryFactory.newBranch(parent, register(missingLevel));
                dirs.put(missingLevel, parent);
            }

            // Rebase the existing root-directory; after this operation it's
            // not a root directory anymore but a sub-directory of the root
            // directory specified.
            final Directory rebasedDirectory = existingRoot.rebase(parent);
            dirs.put(existingRoot.getPath(), rebasedDirectory);

            // This is important: we need to rebase also the direct children of the
            // former root directory otherwise they would reference a invalid parent!
            rebaseDirectSubDirectories(rebasedDirectory);
        }
    }

    /**
     * @param pWatchedDirectory
     * @param pObservers
     */
    // Note: Despite a ConcurrenMap is used it's necessary to synchronize this method.
    // The reason is because all sub-directories need to be registered before another WatchedDirectory
    // is being registered through this method.
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

            // If there are root-directories registered which are children or the newly added
            // root directory, they need to be rebased (including their direct children)
            rebaseExistingRootDirectories(dir);

            // Register directories, ignore existing because they have already been
            // delivered to the observers.
            walkDirectory(directory, pObservers, true);
        }

        // In any case, add the directory key to the directory
        dir.addDirectoryKey(key);
    }

    private Collection<Directory> cancelDiscarded(final Directory pDiscardedParent,
                                                  final Collection<Directory> pToBeConverted) {
        final Collection<Directory> toBeDiscarded = new LinkedList<>();
        dirs.values().removeIf(dir -> {
            if (pDiscardedParent.isDirectParentOf(dir)) {
                if (dir.hasKeys()) {
                    pToBeConverted.add(dir);
                } else {
                    toBeDiscarded.add(dir);
                    dir.cancelKey();
                }
                return true;
            }
            return false;
        });
        toBeDiscarded.forEach(dir -> cancelDiscarded(dir, pToBeConverted));
        return pToBeConverted;
    }

    public synchronized void unregisterRootDirectory(final WatchedDirectory pWatchedDirectory,
                                                     final Collection<FileObserver> pObservers) {
        final Object key = requireNonNull(pWatchedDirectory.getKey(), "Key is null");
        final Path directory = requireNonNull(pWatchedDirectory.getDirectory(), "Directory is null");

        final Directory dir = dirs.get(directory);

        // Remove the directory-key of the watched directory from the key list
        dir.removeDirectoryKey(key);

        // If all watched-directories which referenced the directory have been de-registered,
        // it's time to clean-up.
        if (!dir.hasKeys()) {
            cancelDiscarded(dir, new LinkedList<>()).forEach(d -> {
                dirs.replace(d.getPath(), d.toRootDirectory());
            });
        }

        // Inform observers that a key has been removed
        dir.informDiscard(pObservers, directory, key);
    }

    private void walkDirectory(final Path pDirectory, final Collection<FileObserver> pObservers, final boolean pSkipExisting) {
        // Asynchronously register all sub-directories with the watch-service, and,
        // inform the registered FileObservers
        executorServices.getDirectoryWalkerExecutor().execute(() -> {
            try {
                walkFileTree(pDirectory, new DirectoryInitializerFileVisitor(pSkipExisting, pObservers));
            } catch (final IOException e) {
                LOG.warn(e.getMessage(), e);
            }
        });
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
        walkDirectory(pDirectory, pObservers, false);
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

    /**
     * This visitor is used to walk through a directory structure which needs to be registered
     * with the enclosing {@link DedicatedFileSystem} instance. If a file is detected, the
     * observers specified through the constructor of this class will be informed. If a directory is
     * detected, a new directory object will be created and registered with the enclosing instance.
     */
    private class DirectoryInitializerFileVisitor extends SimpleFileVisitor<Path> {
        private final Collection<FileObserver> observers;
        private final boolean skipExisting;
        private Directory newlyAdded;

        /**
         * Creates a new instance of this class.
         *
         * @param pObservers Observers to be informed about detected files, must not be {@code null}
         */
        public DirectoryInitializerFileVisitor(final boolean pSkipExisting, final Collection<FileObserver> pObservers) {
            observers = pObservers;
            skipExisting = pSkipExisting;
        }

        @Override
        public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
            // It's important here to only trigger the observers if the file has changed.
            // This is most certainly the case, but, there is an exception: because we already
            // registered the parent directory of the file with the watch-service there's a small
            // chance that the file had already been modified before we got here.
            dirs.get(file.getParent()).informIfChanged(observers, file);
            return CONTINUE;
        }

        @Override
        public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
            try {
                dirs.computeIfAbsent(dir,
                        p -> {
                            try {
                                return newlyAdded = directoryFactory.newBranch(dirs.get(dir.getParent()), register(dir));
                            } catch (final IOException e) {
                                throw new UncheckedIOException(e.getMessage(), e);
                            }
                        });
                return skipExisting && dir.equals(newlyAdded.getPath()) ?
                        SKIP_SUBTREE :
                        CONTINUE;
            } catch (final UncheckedIOException e) {
                throw new IOException(e.getMessage(), e);
            }
        }
    }
}
