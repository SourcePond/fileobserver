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
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.lang.String.format;
import static java.nio.file.Files.isDirectory;
import static java.util.Objects.requireNonNull;

/**
 *
 */
public class VirtualRoot {
    private static final Logger LOG = LoggerFactory.getLogger(VirtualRoot.class);
    private final Map<Object, WatchedDirectory> watchtedDirectories = new ConcurrentHashMap<>();
    private final ConcurrentMap<FileSystem, DedicatedFileSystem> children = new ConcurrentHashMap<>();
    private final Set<FileObserver> observers = ConcurrentHashMap.newKeySet();
    private final DedicatedFileSystemFactory dedicatedFileSystemFactory;

    /**
     * We intentionally do <em>not</em> use {@code children.values()} because we need to iterate
     * over the children in the scanner-thread loop. If we used {@code children.values()} directly
     * we must create a new iterator during every loop iteration. This is a potential memory leak
     * and causes unnecessary GC activity.
     */
    private final List<DedicatedFileSystem> roots = new CopyOnWriteArrayList<>();

    // Constructor for BundleActivator
    public VirtualRoot(final DirectoryFactory pDirectoryFactory) {
        this(new DedicatedFileSystemFactory(pDirectoryFactory));
    }

    // Constructor for BundleActivator
    public VirtualRoot(final DedicatedFileSystemFactory pDedicatedFileSystemFactory) {
        dedicatedFileSystemFactory = pDedicatedFileSystemFactory;
    }

    // Composition callback for Felix DM
    public Object[] getComposition() {
        return new Object[]{dedicatedFileSystemFactory, dedicatedFileSystemFactory.getDirectoryFactory()};
    }

    public void addObserver(final FileObserver pObserver) {
        if (null == pObserver) {
            LOG.warn("Observer is null; nothing to add");
        } else if (observers.add(pObserver)) {
            children.values().forEach(f -> f.forceInform(pObserver));
        }
    }

    public void removeObserver(final FileObserver pObserver) {
        if (null == pObserver) {
            LOG.warn("Observer is null; nothing to remove");
        } else {
            observers.remove(pObserver);
        }
    }

    private DedicatedFileSystem newDirectories(final FileSystem pFs) {
        try {
            final DedicatedFileSystem fsdirs = dedicatedFileSystemFactory.newDirectories(pFs);
            roots.add(fsdirs);
            return fsdirs;
        } catch (final IOException e) {
            throw new UncheckedIOException(e.getMessage(), e);
        }
    }

    // This method must be synchronized because all sub-directories need to be
    // registered before another WatchedDirectory is being registered.
    public synchronized void addRoot(final WatchedDirectory pWatchedDirectory) throws IOException {
        // Insure that the directory-key is unique
        final Object key = requireNonNull(pWatchedDirectory.getKey(), "Key is null");
        if (watchtedDirectories.containsKey(key)) {
            throw new IllegalArgumentException(format("Directory-key %s is already used by %s", watchtedDirectories.get(key)));
        }
        watchtedDirectories.put(key, pWatchedDirectory);

        final Path directory = requireNonNull(pWatchedDirectory.getDirectory(), "Directory is null");
        try {
            children.computeIfAbsent(directory.getFileSystem(),
                    this::newDirectories).registerRootDirectory(pWatchedDirectory, observers);
        } catch (final UncheckedIOException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    // This method must be synchronized because all sub-directories need to be
    // discarded before another WatchedDirectory is being unregistered.
    public synchronized void removeRoot(final WatchedDirectory pWatchedDirectory) {
        // It's already checked that nothing is null
        final DedicatedFileSystem fs = children.get(pWatchedDirectory.getDirectory().getFileSystem());

        if (fs == null) {
            LOG.warn(format("Dedicated file system not registered for directory %s! Noting unregistered"));
        } else {
            fs.unregisterRootDirectory(pWatchedDirectory, observers);
            watchtedDirectories.remove(pWatchedDirectory.getKey());
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
            final Directory dir = getDedicatedFileSystem(pPath).getDirectory(pPath.getParent());
            if (dir == null) {
                throw new NullPointerException(format("No directory registered for file %s", pPath));
            }
            dir.informIfChanged(observers, pPath);
        }
    }

    public void pathDiscarded(final Path pPath) {
        final DedicatedFileSystem dfs = getDedicatedFileSystem(pPath);

        // The deleted path was a directory
        if (!dfs.directoryDiscarded(observers, pPath)) {
            final Directory parentDirectory = dfs.getDirectory(pPath.getParent());
            if (parentDirectory == null) {
                throw new NullPointerException(format("Parent directory does not exist for discarded file %s", pPath));
            }

            // The deleted path was a file
            dfs.getDirectory(pPath.getParent()).informDiscard(observers, pPath);
        }
    }

    // Lifecycle method for Felix DM
    public void stop() {
        children.values().forEach(DedicatedFileSystem::close);
        children.clear();
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

            // Remove closed watch-service from the roots-list and children-map
            roots.remove(pDedicatedFileSystem);
            children.values().remove(pDedicatedFileSystem);
        }
    }

    public List<DedicatedFileSystem> getRoots() {
        return roots;
    }
}
