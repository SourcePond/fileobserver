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

import ch.sourcepond.io.fileobserver.api.PathChangeEvent;
import ch.sourcepond.io.fileobserver.impl.directory.Directory;
import ch.sourcepond.io.fileobserver.impl.directory.DirectoryFactory;
import ch.sourcepond.io.fileobserver.impl.listener.DiffEventDispatcher;
import ch.sourcepond.io.fileobserver.impl.listener.EventDispatcher;
import ch.sourcepond.io.fileobserver.impl.listener.ListenerManager;
import ch.sourcepond.io.fileobserver.spi.WatchedDirectory;
import org.slf4j.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.util.concurrent.ConcurrentMap;

import static java.lang.String.format;
import static java.lang.Thread.currentThread;
import static java.nio.file.StandardWatchEventKinds.*;
import static org.slf4j.LoggerFactory.getLogger;

/**
 *
 */
public class DedicatedFileSystem implements Closeable, Runnable {
    private static final Logger LOG = getLogger(DedicatedFileSystem.class);
    public static final Runnable EMPTY_CALLBACK = () -> {};
    private final PathProcessingQueues pathProcessingQueues;
    private final ConcurrentMap<Path, Directory> dirs;
    private final Thread thread;
    private final DirectoryFactory directoryFactory;
    private final WatchServiceWrapper wrapper;
    private final DirectoryRebase rebase;
    private final ListenerManager manager;
    private final PathChangeHandler pathChangeHandler;

    DedicatedFileSystem(final PathProcessingQueues pPathProcessingQueues,
                        final DirectoryFactory pDirectoryFactory,
                        final WatchServiceWrapper pWrapper,
                        final DirectoryRebase pRebase,
                        final ListenerManager pManager,
                        final PathChangeHandler pPathChangeHandler,
                        final ConcurrentMap<Path, Directory> pDirs) {
        pathProcessingQueues = pPathProcessingQueues;
        pathChangeHandler = pPathChangeHandler;
        directoryFactory = pDirectoryFactory;
        wrapper = pWrapper;
        rebase = pRebase;
        manager = pManager;
        dirs = pDirs;
        thread = new Thread(this, format("fileobserver %s", this));
    }

    /**
     * <p>Iterates through all registered directories and passes all their files to the
     * {@link ch.sourcepond.io.fileobserver.api.PathChangeListener#modified(PathChangeEvent)} of the observer specified. This is necessary for newly registered
     * observers who need to know about all watched files. See {@link #registerRootDirectory(EventDispatcher, WatchedDirectory)} and
     * {@link PathChangeHandler#pathModified(EventDispatcher, Path, Runnable, boolean)} to get an idea how directories are registered with this object.
     * <p>Note: it's guaranteed that the {@link Path} instances passed
     * to the observer are regular files (not directories).
     */
    public void forceInform(final EventDispatcher pDispatcher) {
        dirs.values().forEach(d -> d.forceInform(pDispatcher));
    }

    /**
     * This method is <em>not</em> thread-safe and must be synchronized externally.
     *
     * @param pWatchedDirectory
     */
    public void registerRootDirectory(final WatchedDirectory pWatchedDirectory)
            throws IOException {
        registerRootDirectory(manager.getDefaultDispatcher(), pWatchedDirectory);
    }

    private synchronized void registerRootDirectory(final EventDispatcher pDispatcher, final WatchedDirectory pWatchedDirectory)
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
            // (otherwise PathChangeListener#supplement would not be called).
            pathChangeHandler.rootAdded(pDispatcher, dir);
        }

        // VERY IMPORTANT: in any case, associate the directory with the watched-directory
        dir.addWatchedDirectory(pWatchedDirectory);
    }

    /**
     * This method is <em>not</em> thread-safe and must be synchronized externally.
     *
     * @param pWatchedDirectory
     */
    public void unregisterRootDirectory(
            final Path pToBeUnregistered,
            final WatchedDirectory pWatchedDirectory) {
        unregisterRootDirectory(manager.getDefaultDispatcher(), pToBeUnregistered, pWatchedDirectory);
    }

    private void unregisterRootDirectory(
            final EventDispatcher pDispatcher,
            final Path pToBeUnregistered,
            final WatchedDirectory pWatchedDirectory) {
        // It's already checked that the directory-key and the directory are not null
        final Directory dir = dirs.get(pToBeUnregistered);
        if (dir == null) {
            LOG.warn(format("Directory %s is unknown; nothing unregistered", pWatchedDirectory.getDirectory()));
        } else {
            // Remove the directory-key of the watched directory from the key list
            dir.removeWatchedDirectory(pDispatcher, pWatchedDirectory);

            // If all watched-directories which referenced the directory have been de-registered,
            // it's time to clean-up.
            if (!dir.hasKeys()) {
                rebase.cancelAndRebaseDiscardedDirectory(dir);
            }
        }
    }

    public Directory getDirectory(final Path pPath) {
        return dirs.get(pPath);
    }

    public void destinationChanged(final WatchedDirectory pWatchedDirectory,
                                   final Path pPrevious) throws IOException {
        final Directory dir = dirs.get(pPrevious);
        if (dir == null) {
            LOG.warn("Destination change has no effect because no directory found for previous path {}");
        } else {
            // Unregister and register watched-directory. IMPORTANT: do not
            // inform observers at all, this will be handled later!
            try (final DiffEventDispatcher dispatcher = manager.openDiff(this)) {
                unregisterRootDirectory(dispatcher, pPrevious, pWatchedDirectory);
                registerRootDirectory(dispatcher, pWatchedDirectory);
            }

            LOG.info("Destination changed from {} to {}", pPrevious, pWatchedDirectory.getDirectory());
        }
    }

    /**
     * <p>Stops the scanner thread which observes the watched directories for changes.</p>
     * <p>
     * <p>This must be named "stop" in order to be called from Felix DM (see
     * <a href="http://felix.apache.org/documentation/subprojects/apache-felix-dependency-manager/reference/components.html">Dependency Manager - Components</a>)</p>
     */
    @Override
    public void close() {
        if (!thread.isInterrupted()) {
            try {
                thread.interrupt();
                LOG.info("Event receiver stopped");
            } finally {
                try {
                    wrapper.close();
                } finally {
                    dirs.clear();
                    pathChangeHandler.removeFileSystem(this);
                }
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

    private synchronized void processPath(final WatchEvent.Kind<?> pKind,
                             final Path child,
                             final Runnable pDoneCallback) {
        LOG.debug("Received event of kind {} for path {}", pKind, child);
        try {
            if (ENTRY_CREATE == pKind) {
                pathChangeHandler.pathModified(manager.getDefaultDispatcher(), child, pDoneCallback, true);
            } else if (ENTRY_MODIFY == pKind) {
                pathChangeHandler.pathModified(manager.getDefaultDispatcher(), child, pDoneCallback, false);
            } else if (ENTRY_DELETE == pKind) {
                pathChangeHandler.pathDiscarded(manager.getDefaultDispatcher(), child, pDoneCallback);
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

            pathProcessingQueues.enque(directory, event, this::processPath);
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
