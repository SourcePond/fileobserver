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
import ch.sourcepond.io.fileobserver.impl.directory.Directory;
import ch.sourcepond.io.fileobserver.impl.directory.DirectoryFactory;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;

import static java.lang.String.format;
import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.Files.walkFileTree;
import static java.util.Objects.requireNonNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * <p>Walks through a directory structure. For each detected file, the observers specified
 * will be informed through their {@link FileObserver#modified(FileKey, Path)}. Each detected
 * directory will be registered with the {@link WatchServiceWrapper}, and, be stored in the
 * directory-map specified.</p>
 * <p>
 * <p>There should exist exactly one instance of this class (singleton).</p>
 */
class DirectoryRegistrationWalker {
    private final Logger logger;
    private final WatchServiceWrapper wrapper;
    private final DirectoryFactory directoryFactory;
    private final ConcurrentMap<Path, Directory> dirs;
    private final ExecutorService directoryWalkerExecutor;

    /**
     * Constructor for bundle activator.
     *
     * @param pWrapper
     * @param pDirectoryFactory
     * @param pDirectoryWalkterExecutor
     * @param pDirs
     */
    DirectoryRegistrationWalker(final WatchServiceWrapper pWrapper,
                                final DirectoryFactory pDirectoryFactory,
                                final ExecutorService pDirectoryWalkterExecutor,
                                final ConcurrentMap<Path, Directory> pDirs) {
        logger = getLogger(getClass());
        wrapper = pWrapper;
        directoryFactory = pDirectoryFactory;
        directoryWalkerExecutor = pDirectoryWalkterExecutor;
        dirs = pDirs;
    }

    /**
     * Constructor for testing
     *
     * @param pLogger
     * @param pDirectoryWalkerExecutor
     * @param pWrapper
     * @param pDirectoryFactory
     * @param pDirs
     */
    DirectoryRegistrationWalker(final Logger pLogger,
                                final ExecutorService pDirectoryWalkerExecutor,
                                final WatchServiceWrapper pWrapper,
                                final DirectoryFactory pDirectoryFactory,
                                final ConcurrentMap<Path, Directory> pDirs) {
        logger = pLogger;
        directoryWalkerExecutor = pDirectoryWalkerExecutor;
        wrapper = pWrapper;
        directoryFactory = pDirectoryFactory;
        dirs = pDirs;
    }

    /**
     * Registers the directory specified and all its sub-directories with the watch-service held by this object.
     * Additionally, it passes any detected file to {@link FileObserver#modified(FileKey, Path)} to the observers
     * specified.
     *
     * @param pDirectory Newly created directory, must not be {@code null}
     * @param pObservers Observers to be informed about detected files, must not be {@code null}
     */
    void directoryCreated(final Path pDirectory,
                          final Collection<FileObserver> pObservers) {
        directoryCreated(null, pDirectory, pObservers);
    }

    /**
     * Registers the directory specified and all its sub-directories with the watch-service held by this object.
     * Additionally, it passes any detected file to {@link FileObserver#modified(FileKey, Path)} to the observers
     * specified.
     *
     * @param pNewRoot   Newly created directory, must not be {@code null}
     * @param pObservers Observers to be informed about detected files, must not be {@code null}
     */
    void rootAdded(final Directory pNewRoot,
                   final Collection<FileObserver> pObservers) {
        directoryCreated(pNewRoot, pNewRoot.getPath(), pObservers);
    }

    /**
     * Registers the directory specified and all its sub-directories with the watch-service held by this object.
     * Additionally, it passes any detected file to {@link FileObserver#modified(FileKey, Path)} to the observers
     * specified.
     *
     * @param pNewRootOrNull New root-directory which causes a rebase, or, {@code null}
     * @param pDirectory     Newly created directory, must not be {@code null}
     * @param pObservers     Observers to be informed about detected files, must not be {@code null}
     */
    private void directoryCreated(final Directory pNewRootOrNull,
                                  final Path pDirectory,
                                  final Collection<FileObserver> pObservers) {
        // Asynchronously register all sub-directories with the watch-service, and,
        // inform the registered FileObservers
        directoryWalkerExecutor.execute(() -> {
            try {
                walkFileTree(pDirectory, new DirectoryInitializerFileVisitor(pNewRootOrNull, pObservers));
            } catch (final IOException e) {
                logger.warn(e.getMessage(), e);
            } catch (final RuntimeException e) {
                logger.error(e.getMessage(), e);
            }
        });
    }

    /**
     * This visitor is used to walk through a directory structure which needs to be registered
     * with the enclosing {@link DedicatedFileSystem} instance. If a file is detected, the
     * observers specified through the constructor of this class will be informed. If a directory is
     * detected, a new directory object will be created and registered with the enclosing instance.
     */
    private class DirectoryInitializerFileVisitor extends SimpleFileVisitor<Path> {
        private final Directory newRootOrNull;
        private final Collection<FileObserver> observers;

        /**
         * Creates a new instance of this class.
         *
         * @param pObservers Observers to be informed about detected files, must not be {@code null}
         */
        public DirectoryInitializerFileVisitor(final Directory pNewRootOrNull,
                                               final Collection<FileObserver> pObservers) {
            newRootOrNull = pNewRootOrNull;
            observers = pObservers;
        }

        @Override
        public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) {
            // It's important here to only trigger the observers if the file has changed.
            // This is most certainly the case, but, there is an exception: because we already
            // registered the parent directory of the file with the watch-service there's a small
            // chance that the file had already been modified before we got here.
            dirs.get(file.getParent()).informIfChanged(newRootOrNull, observers, file);
            return CONTINUE;
        }

        @Override
        public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
            try {
                // Only put a new directory if not already present. This is important, otherwise
                // multiple threads would overwrite them.
                dirs.computeIfAbsent(dir, this::createBranch);
                return CONTINUE;
            } catch (final UncheckedIOException e) {
                throw new IOException(e.getMessage(), e);
            }
        }

        private Directory createBranch(final Path pDir) {
            final Directory parentDir = requireNonNull(dirs.get(pDir.getParent()), () -> format("No parent registered for %s", pDir));
            final Directory newDirectory;
            try {
                newDirectory = directoryFactory.newBranch(parentDir, wrapper.register(pDir));
            } catch (final IOException e) {
                throw new UncheckedIOException(e.getMessage(), e);
            }
            return newDirectory;
        }
    }
}
