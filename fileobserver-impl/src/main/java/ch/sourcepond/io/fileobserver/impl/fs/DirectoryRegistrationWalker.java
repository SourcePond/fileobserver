package ch.sourcepond.io.fileobserver.impl.fs;

import ch.sourcepond.io.fileobserver.api.FileKey;
import ch.sourcepond.io.fileobserver.api.FileObserver;
import ch.sourcepond.io.fileobserver.impl.ExecutorServices;
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

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.Files.walkFileTree;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Created by rolandhauser on 06.03.17.
 */
class DirectoryRegistrationWalker {
    private static final Logger LOG = getLogger(DirectoryRegistrationWalker.class);
    private final ExecutorServices executorServices;
    private final WatchServiceWrapper wrapper;
    private final DirectoryFactory directoryFactory;
    private final ConcurrentMap<Path, Directory> dirs;

    DirectoryRegistrationWalker(final ExecutorServices pExecutorServices,
                                final WatchServiceWrapper pWrapper,
                                final DirectoryFactory pDirectoryFactory,
                                final ConcurrentMap<Path, Directory> pDirs) {
        executorServices = pExecutorServices;
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
    void directoryCreated(final Path pDirectory, final Collection<FileObserver> pObservers) {
        // Asynchronously register all sub-directories with the watch-service, and,
        // inform the registered FileObservers
        executorServices.getDirectoryWalkerExecutor().execute(() -> {
            try {
                walkFileTree(pDirectory, new DirectoryInitializerFileVisitor(pObservers));
            } catch (final IOException e) {
                LOG.warn(e.getMessage(), e);
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
        private final Collection<FileObserver> observers;

        /**
         * Creates a new instance of this class.
         *
         * @param pObservers Observers to be informed about detected files, must not be {@code null}
         */
        public DirectoryInitializerFileVisitor(final Collection<FileObserver> pObservers) {
            observers = pObservers;
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
                                return directoryFactory.newBranch(dirs.get(dir.getParent()), wrapper.register(dir));
                            } catch (final IOException e) {
                                throw new UncheckedIOException(e.getMessage(), e);
                            }
                        });
                return CONTINUE;
            } catch (final UncheckedIOException e) {
                throw new IOException(e.getMessage(), e);
            }
        }
    }
}
