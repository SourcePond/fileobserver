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
import ch.sourcepond.io.fileobserver.impl.diff.DiffObserverFactory;
import ch.sourcepond.io.fileobserver.impl.directory.Directory;
import ch.sourcepond.io.fileobserver.impl.directory.DirectoryFactory;
import ch.sourcepond.io.fileobserver.impl.filekey.DefaultFileKeyFactory;
import ch.sourcepond.io.fileobserver.spi.RelocationObserver;
import ch.sourcepond.io.fileobserver.spi.WatchedDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.lang.String.format;
import static java.nio.file.Files.isDirectory;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.ConcurrentHashMap.newKeySet;

/**
 *
 */
public class VirtualRoot implements RelocationObserver {
    private static final Logger LOG = LoggerFactory.getLogger(VirtualRoot.class);
    private final Map<Object, WatchedDirectory> watchtedDirectories = new ConcurrentHashMap<>();
    private final ConcurrentMap<FileSystem, DedicatedFileSystem> children = new ConcurrentHashMap<>();
    private final Set<FileObserver> observers = newKeySet();
    private final DedicatedFileSystemFactory dedicatedFileSystemFactory;


    // Constructor for BundleActivator
    public VirtualRoot() {
        final DefaultFileKeyFactory keyFactory = new DefaultFileKeyFactory();
        dedicatedFileSystemFactory = new DedicatedFileSystemFactory(
                new DirectoryFactory(keyFactory),
                new DiffObserverFactory(keyFactory));
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
        return new Object[]{
                this,
                dedicatedFileSystemFactory,
                dedicatedFileSystemFactory.getDirectoryFactory(),
                dedicatedFileSystemFactory.getDiffObserverFactory()};
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

    private DedicatedFileSystem newDedicatedFileSystem(final FileSystem pFs) {
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
                    this::newDedicatedFileSystem).registerRootDirectory(pWatchedDirectory, observers);
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

    /**
     *
     */
    @Override
    public synchronized void destinationChanged(final WatchedDirectory pWatchedDirectory, final Path pPrevious) throws IOException {
        final Object key = requireNonNull(pWatchedDirectory.getKey(), "Key is null");
        final Path directory = requireNonNull(pWatchedDirectory.getDirectory(), "Directory is null");

        if (watchtedDirectories.replace(key, pWatchedDirectory) != null) {
            getDedicatedFileSystem(directory).destinationChanged(
                    pWatchedDirectory, pPrevious, observers);
        } else {
            LOG.warn("Directory with key {} was not mapped; nothing changed", directory);
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
        if (isDirectory(pPath)) {
            getDedicatedFileSystem(pPath).directoryCreated(pPath, observers);
        } else {
            final Directory dir = requireNonNull(
                    getDedicatedFileSystem(pPath).getDirectory(pPath.getParent()),
                    () -> format("No directory registered for file %s", pPath));
            dir.informIfChanged(observers, pPath);
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

    /**
     * <p>Closes all active dedicated files systems and removes them.</p>
     * <p>This must be named "destroy" in order to be called from Felix DM (see
     * <a href="http://felix.apache.org/documentation/subprojects/apache-felix-dependency-manager/reference/components.html">Dependency Manager - Components</a>)</p>
     */
    // Lifecycle method for Felix DM
    public void stop() {
        children.values().forEach(DedicatedFileSystem::close);
        children.clear();
        LOG.info("Timestamp cleaner stopped");
    }

    /**
     * Removes the {@link DedicatedFileSystem} instance specified from
     * this virtual root. This happens when the {@link java.nio.file.WatchService} of the
     * fs specified had been closed and a {@link java.nio.file.ClosedWatchServiceException} has been
     * caused to be thrown because that in {@link DedicatedFileSystem#run()}.
     *
     * @param pDedicatedFileSystem
     */
    void removeFileSystem(final DedicatedFileSystem pDedicatedFileSystem) {
        children.values().remove(pDedicatedFileSystem);
    }
}
