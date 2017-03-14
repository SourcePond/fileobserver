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
import ch.sourcepond.io.fileobserver.impl.directory.Directory;
import ch.sourcepond.io.fileobserver.impl.directory.DirectoryFactory;
import ch.sourcepond.io.fileobserver.spi.WatchedDirectory;
import org.slf4j.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.attribute.FileTime;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.lang.String.format;
import static java.lang.Thread.currentThread;
import static java.nio.file.Files.getLastModifiedTime;
import static java.nio.file.StandardWatchEventKinds.*;
import static org.slf4j.LoggerFactory.getLogger;

/**
 *
 */
public class DedicatedFileSystem implements Closeable, Runnable {
    private static final Logger LOG = getLogger(DedicatedFileSystem.class);
    private final ConcurrentMap<Path, FileTime> timestamps = new ConcurrentHashMap<>();
    private final ConcurrentMap<Path, Directory> dirs;
    private final Thread thread;
    private final VirtualRoot virtualRoot;
    private final DirectoryFactory directoryFactory;
    private final WatchServiceWrapper wrapper;
    private final DirectoryRebase rebase;
    private final DirectoryRegistrationWalker walker;

    DedicatedFileSystem(final VirtualRoot pVirtualRoot,
                        final DirectoryFactory pDirectoryFactory,
                        final WatchServiceWrapper pWrapper,
                        final DirectoryRebase pRebase,
                        final DirectoryRegistrationWalker pWalker,
                        final ConcurrentMap<Path, Directory> pDirs) {
        virtualRoot = pVirtualRoot;
        directoryFactory = pDirectoryFactory;
        wrapper = pWrapper;
        rebase = pRebase;
        walker = pWalker;
        dirs = pDirs;
        thread = new Thread(this, format("fileobserver %s", this));
    }

    /**
     * <p>Iterates through all registered directories and passes all their files to the
     * {@link FileObserver#modified(ch.sourcepond.io.fileobserver.api.FileKey, Path)} of the observer specified. This is necessary for newly registered
     * observers who need to know about all watched files. See {@link #registerRootDirectory(WatchedDirectory, Collection)} and
     * {@link #directoryCreated(Path, Collection)} to get an idea how directories are registered with this object.
     * <p>Note: it's guaranteed that the {@link Path} instances passed
     * to the observer are regular files (not directories).
     *
     * @param pObserver Observer to be informed, must not be {@code null}.
     */
    public void forceInform(final FileObserver pObserver) {
        dirs.values().forEach(d -> d.forceInform(pObserver));
    }

    /**
     * This method is <em>not</em> thread-safe and must be synchronized externally.
     *
     * @param pWatchedDirectory
     * @param pObservers
     */
    void registerRootDirectory(final WatchedDirectory pWatchedDirectory,
                               final Collection<FileObserver> pObservers)
            throws IOException {
        // It's already checked that the directory is not null
        final Path directory = pWatchedDirectory.getDirectory();

        Directory dir = dirs.get(directory);
        if (dir == null) {
            // If no directory is registered for the path specified, create a new root-directory.
            // Register the path with the watch-service and use the returned watch-key to create the root directory.
            dir = directoryFactory.newRoot(wrapper.register(directory));

            // If there are root-directories registered which are children or the newly added
            // root directory, they need to be rebased (including their direct children)
            rebase.rebaseExistingRootDirectories(dir);

            // Register directories; important here is to pass the newly created root-directory
            // (otherwise FileObserver#supplement would not be called).
            walker.rootAdded(dir, pObservers);
        }

        // VERY IMPORTANT: in any case, add the directory with the directory-key
        dir.addDirectoryKey(pWatchedDirectory.getKey());
    }

    /**
     * This method is <em>not</em> thread-safe and must be synchronized externally.
     *
     * @param pWatchedDirectory
     * @param pObservers
     */
    void unregisterRootDirectory(final WatchedDirectory pWatchedDirectory,
                                 final Collection<FileObserver> pObservers) {
        // It's already checked that the directory-key and the directory are not null
        final Directory dir = dirs.get(pWatchedDirectory.getDirectory());
        if (dir == null) {
            LOG.warn(format("Directory %s is unknown; nothing unregistered", pWatchedDirectory.getDirectory()));
        } else {
            // Remove the directory-key of the watched directory from the key list
            dir.removeDirectoryKey(pWatchedDirectory.getKey(), pObservers);

            // If all watched-directories which referenced the directory have been de-registered,
            // it's time to clean-up.
            if (!dir.hasKeys()) {
                rebase.cancelAndRebaseDiscardedDirectory(dir);
            }
        }
    }

    /**
     * Registers the directory specified and all its sub-directories with the watch-service held by this object.
     * Additionally, it passes any detected file to {@link FileObserver#modified(ch.sourcepond.io.fileobserver.api.FileKey, Path)} to the observers
     * specified.
     *
     * @param pDirectory Newly created directory, must not be {@code null}
     * @param pObservers Observers to be informed about detected files, must not be {@code null}
     */
    void directoryCreated(final Path pDirectory, final Collection<FileObserver> pObservers) {
        walker.directoryCreated(pDirectory, pObservers);
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

    /**
     * <p>Stops the scanner thread which observes the watched directories for changes.</p>
     * <p>
     * <p>This must be named "stop" in order to be called from Felix DM (see
     * <a href="http://felix.apache.org/documentation/subprojects/apache-felix-dependency-manager/reference/components.html">Dependency Manager - Components</a>)</p>
     */
    @Override
    public void close() {
        try {
            thread.interrupt();
            LOG.info("Event receiver stopped");
        } finally {
            try {
                wrapper.close();
            } finally {
                dirs.clear();
                timestamps.clear();
                virtualRoot.removeFileSystem(this);
            }
        }
    }

    @Override
    public String toString() {
        return wrapper.toString();
    }

    /**
     * <p>Starts the scanner thread which observes the watched directories for changes.</p>
     * <p>
     * <p>This must be named "start" in order to be called from Felix DM (see
     * <a href="http://felix.apache.org/documentation/subprojects/apache-felix-dependency-manager/reference/components.html">Dependency Manager - Components</a>)</p>
     */
    // Lifecycle method for Felix DM
    public void start() {
        thread.setDaemon(true);
        thread.start();
        LOG.info("Ready for receiving events");
    }

    private boolean hasChanged(final Path pPath) {
        boolean changed = false;
        try {
            final FileTime current = getLastModifiedTime(pPath);
            final FileTime cachedOrNull = timestamps.putIfAbsent(pPath, current);
            changed = !current.equals(cachedOrNull);

            if (cachedOrNull != null && changed) {
                timestamps.replace(pPath, cachedOrNull, current);
            }
        } catch (final IOException e) {
            LOG.warn("Modification time could not be determined!", e);
        }
        return changed;
    }

    private void processPath(final WatchEvent.Kind<?> pKind, final Path child) {
        try {
            // The filename is the
            // context of the event.
            if ((ENTRY_CREATE == pKind || ENTRY_MODIFY == pKind) && hasChanged(child)) {
                virtualRoot.pathModified(child);
            } else if (ENTRY_DELETE == pKind) {
                try {
                    virtualRoot.pathDiscarded(child);
                } finally {
                    timestamps.remove(child);
                }
            }
        } catch (final RuntimeException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    private void processEvent(final WatchKey pWatchKey) {
        final Path directory = (Path) pWatchKey.watchable();
        for (final WatchEvent<?> event : pWatchKey.pollEvents()) {
            final WatchEvent.Kind<?> kind = event.kind();

            if (LOG.isDebugEnabled()) {
                LOG.debug(format("Changed detected [%s]: %s, context: %s", kind, directory, event.context()));
            }

            // An OVERFLOW event can
            // occur regardless if events
            // are lost or discarded.
            if (OVERFLOW == kind) {
                continue;
            }

            processPath(kind, directory.resolve((Path) event.context()));
        }

        // The case when the WatchKey has been cancelled is
        // already handled at a different place.
        pWatchKey.reset();
    }

    @Override
    public void run() {
        while (!currentThread().isInterrupted()) {
            try {
                processEvent(wrapper.take());
            } catch (final ClosedWatchServiceException | InterruptedException e) {
                LOG.debug(e.getMessage(), e);
                close();
            }
        }
    }
}
