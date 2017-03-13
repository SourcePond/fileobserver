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
import ch.sourcepond.io.fileobserver.spi.RelocationObserver;
import ch.sourcepond.io.fileobserver.spi.WatchedDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.lang.String.format;
import static java.lang.Thread.currentThread;
import static java.nio.file.Files.getLastModifiedTime;
import static java.nio.file.Files.isDirectory;
import static java.time.Clock.systemUTC;
import static java.time.Instant.now;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.ConcurrentHashMap.newKeySet;

/**
 *
 */
public class VirtualRoot implements RelocationObserver, Runnable {
    // TODO: Replace this constant through a configurable value
    private static final int TIMEOUT = 30000;

    private static final Logger LOG = LoggerFactory.getLogger(VirtualRoot.class);
    private final Map<Object, WatchedDirectory> watchtedDirectories = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Path, FileTime> timestamps = new ConcurrentHashMap<>();
    private final ConcurrentMap<FileSystem, DedicatedFileSystem> children = new ConcurrentHashMap<>();
    private final Set<FileObserver> observers = newKeySet();
    private final Thread cleanerThread = new Thread(this, "fileobserver - timestamp-cleaner");
    private final DedicatedFileSystemFactory dedicatedFileSystemFactory;
    private final Clock clock = systemUTC();

    // Constructor for BundleActivator
    public VirtualRoot() {
        this(new DedicatedFileSystemFactory(new DirectoryFactory()));
    }

    // Constructor for BundleActivator
    public VirtualRoot(final DedicatedFileSystemFactory pDedicatedFileSystemFactory) {
        dedicatedFileSystemFactory = pDedicatedFileSystemFactory;
    }

    /**
     * Returns the objects which need to have service dependencies injected. This method
     * must not be renamed!
     *
     * @return
     */
    // Composition callback for Felix DM
    public Object[] getComposition() {
        return new Object[]{this, dedicatedFileSystemFactory, dedicatedFileSystemFactory.getDirectoryFactory()};
    }

    /**
     * Whiteboard bind-method for {@link FileObserver} services exported by any client bundle. This
     * method is called when a client exports a service which implements the {@link FileObserver} interface.
     *
     * @param pObserver File observer service to be registered.
     */
    void addObserver(final FileObserver pObserver) {
        requireNonNull(pObserver, "Observer is null");
        observers.add(pObserver);
        children.values().forEach(f -> f.forceInform(pObserver));
    }

    /**
     * Whiteboard unbind-method {@link FileObserver} services exported by any client bundle. This method is
     * called when a client unregisters a service which implements the {@link FileObserver} interface.
     *
     * @param pObserver File observer service to be unregistered.
     */
    void removeObserver(final FileObserver pObserver) {
        requireNonNull(pObserver, "Observer is null");
        observers.remove(pObserver);
    }

    private DedicatedFileSystem newDirectories(final FileSystem pFs) {
        try {
            return dedicatedFileSystemFactory.openFileSystem(this, pFs);
        } catch (final IOException e) {
            throw new UncheckedIOException(e.getMessage(), e);
        }
    }

    /**
     * Whiteboard bind-method for {@link WatchedDirectory} services exported by any client bundle. This
     * method is called when a client exports a service which implements the {@link WatchedDirectory} interface.
     *
     * @param pWatchedDirectory Watched-directory service to be registered.
     * @throws IOException Thrown, if the root directory could not be added.
     */
    // This method must be synchronized because all sub-directories need to be
    // registered before another WatchedDirectory is being registered.
    public synchronized void addRoot(final WatchedDirectory pWatchedDirectory) throws IOException {
        requireNonNull(pWatchedDirectory, "Watched directory is null");
        final Object key = requireNonNull(pWatchedDirectory.getKey(), "Key is null");
        final Path directory = requireNonNull(pWatchedDirectory.getDirectory(), "Directory is null");

        if (!isDirectory(directory)) {
            throw new IllegalArgumentException(format("[%s]: %s is not a directory!", key, directory));
        }

        // Insure that the directory-key is unique
        if (watchtedDirectories.containsKey(key)) {
            throw new IllegalArgumentException(format("Key %s already used by %s", key, watchtedDirectories.get(key)));
        }
        watchtedDirectories.put(key, pWatchedDirectory);

        try {
            children.computeIfAbsent(directory.getFileSystem(),
                    this::newDirectories).registerRootDirectory(pWatchedDirectory, observers);
        } catch (final UncheckedIOException e) {
            throw new IOException(e.getMessage(), e);
        }

        pWatchedDirectory.addObserver(this);
        LOG.info("Added [{}:{}]", key, directory);
    }

    /**
     * Whiteboard unbind-method {@link WatchedDirectory} services exported by any client bundle. This method is
     * called when a client unregisters a service which implements the {@link WatchedDirectory} interface.
     *
     * @param pWatchedDirectory Watched-directory service to be unregistered.
     */
    // This method must be synchronized because all sub-directories need to be
    // discarded before another WatchedDirectory is being unregistered.
    synchronized void removeRoot(final WatchedDirectory pWatchedDirectory) {
        requireNonNull(pWatchedDirectory, "Watched directory is null");
        final Object key = requireNonNull(pWatchedDirectory.getKey(), "Key is null");
        final Path directory = requireNonNull(pWatchedDirectory.getDirectory(), "Directory is null");

        // It's already checked that nothing is null
        final DedicatedFileSystem fs = children.get(directory.getFileSystem());

        if (fs == null) {
            LOG.warn(format("No dedicated file system registered! Path: %s", directory));
        } else {
            fs.unregisterRootDirectory(pWatchedDirectory, observers);

            // IMPORTANT: remove watched-directory with key specified.
            watchtedDirectories.remove(key);
            pWatchedDirectory.removeObserver(this);
            LOG.info("Removed [{}:{}]", key, directory);
        }
    }

    private DedicatedFileSystem getDedicatedFileSystem(final Path pPath) {
        final DedicatedFileSystem fsdirs = children.get(pPath.getFileSystem());
        if (null == fsdirs) {
            throw new IllegalStateException(format("No appropriate root-directory found for %s", pPath));
        }
        return fsdirs;
    }

    public void pathModified(final Path pPath) {
        if (hasChanged(pPath)) {
            if (isDirectory(pPath)) {
                getDedicatedFileSystem(pPath).directoryCreated(pPath, observers);
            } else {
                final Directory dir = requireNonNull(
                        getDedicatedFileSystem(pPath).getDirectory(pPath.getParent()),
                        () -> format("No directory registered for file %s", pPath));
                dir.informIfChanged(observers, pPath);
            }
        }
    }

    public void pathDiscarded(final Path pPath) {
        final DedicatedFileSystem dfs = getDedicatedFileSystem(pPath);

        // The deleted path was a directory
        if (!dfs.directoryDiscarded(observers, pPath)) {
            final Directory parentDirectory = dfs.getDirectory(pPath.getParent());
            if (parentDirectory == null) {
                LOG.warn("Parent of {} does not exist. Nothing to discard", pPath, new Exception());
            } else {
                // The deleted path was a file
                parentDirectory.informDiscard(observers, pPath);
            }
        }
    }

    // Lifecycle method for Felix DM
    public void start() {
        cleanerThread.setDaemon(true);
        cleanerThread.start();
        LOG.info("Virtual root started");
    }

    /**
     * <p>Closes all active dedicated files systems and removes them.</p>
     * <p>This must be named "destroy" in order to be called from Felix DM (see
     * <a href="http://felix.apache.org/documentation/subprojects/apache-felix-dependency-manager/reference/components.html">Dependency Manager - Components</a>)</p>
     */
    // Lifecycle method for Felix DM
    public void stop() {
        cleanerThread.interrupt();
        children.values().forEach(DedicatedFileSystem::close);
        children.clear();
        LOG.info("Timestamp cleaner stopped");
    }

    /**
     * Closes the {@link DedicatedFileSystem} instance specified and removes it from
     * the root list.
     *
     * @param pDedicatedFileSystem
     */
    public void close(final DedicatedFileSystem pDedicatedFileSystem) {
        if (pDedicatedFileSystem != null) {
            pDedicatedFileSystem.close();
            children.values().remove(pDedicatedFileSystem);
        }
    }

    /**
     *
     * @param pWatchedDirectory The watched-directory which has a new destination, never {@code null}
     * @param pPrevious Previous destination, never {@code null}
     */
    @Override
    public void destinationChanged(final WatchedDirectory pWatchedDirectory, final Path pPrevious) {
        // TODO: To be implemented
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

    private boolean waitForNextIteration() {
        synchronized (this) {
            final long nextRun = clock.millis() + TIMEOUT;
            while (nextRun > clock.millis()) {
                try {
                    wait(TIMEOUT);
                } catch (final InterruptedException e) {
                    currentThread().interrupt();
                }
            }
        }
        return !currentThread().isInterrupted();
    }

    @Override
    public void run() {
        while (waitForNextIteration()) {
            final Instant expiration = now().minusMillis(TIMEOUT);
            timestamps.values().removeIf(timestamp -> expiration.isAfter(timestamp.toInstant()));
        }
    }
}
