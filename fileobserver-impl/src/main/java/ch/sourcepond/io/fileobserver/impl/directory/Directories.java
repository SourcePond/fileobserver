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
package ch.sourcepond.io.fileobserver.impl.directory;

import ch.sourcepond.io.fileobserver.api.FileKey;
import ch.sourcepond.io.fileobserver.api.FileObserver;
import ch.sourcepond.io.fileobserver.impl.ExecutorServices;
import ch.sourcepond.io.fileobserver.impl.registrar.Registrar;
import ch.sourcepond.io.fileobserver.impl.registrar.RegistrarFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.lang.String.format;
import static java.nio.file.Files.isDirectory;

/**
 *
 */
public class Directories {
    private static final Logger LOG = LoggerFactory.getLogger(Directories.class);
    private final ConcurrentMap<FileSystem, FsDirectories> children = new ConcurrentHashMap<>();
    private final Set<FileObserver> observers = ConcurrentHashMap.newKeySet();

    private final RegistrarFactory registrarFactory;
    private final FsDirectoriesFactory fsDirectoriesFactory;

    // Injected by Felix DM
    private final ExecutorServices executorServices;

    /**
     * We intentionally do <em>not</em> use {@code children.values()} because we need to iterate
     * over the children in the scanner-thread loop. If we used {@code children.values()} directly
     * we must create a new iterator during every loop iteration. This is a potential memory leak
     * and causes unnecessary GC activity.
     */
    private final List<FsDirectories> roots;

    // Constructor for BundleActivator
    public Directories(final ExecutorServices pExecutorServices, final FsDirectoryFactory pFsDirectoryFactory) {
        executorServices = pExecutorServices;
        registrarFactory = new RegistrarFactory(pExecutorServices, pFsDirectoryFactory);
        fsDirectoriesFactory = new FsDirectoriesFactory();
        roots = new CopyOnWriteArrayList<>();
    }

    // Constructor for testing
    public Directories(final RegistrarFactory pRegistrarFactory,
                       final FsDirectoriesFactory pFsDirectories,
                       final ExecutorServices pExecutorServices,
                       final List<FsDirectories> pRoots) {
        registrarFactory = pRegistrarFactory;
        fsDirectoriesFactory = pFsDirectories;
        executorServices = pExecutorServices;
        roots = pRoots;
    }

    // Composition callback for Felix DM
    public Object[] getComposition() {
        return new Object[]{registrarFactory, registrarFactory.getDirectoryFactory()};
    }

    public void addObserver(final FileObserver pObserver) {
        if (null == pObserver) {
            LOG.warn("Observer is null; nothing to add");
        } else if (observers.add(pObserver)) {
            children.values().forEach(f -> f.initiallyInformHandler(pObserver));
        }
    }

    public void removeObserver(final FileObserver pObserver) {
        if (null == pObserver) {
            LOG.warn("Observer is null; nothing to remove");
        } else {
            observers.remove(pObserver);
        }
    }

    private Registrar newRegistrar(final FileSystem pFs) {
        try {
            return registrarFactory.newRegistrar(pFs);
        } catch (final IOException e) {
            throw new UncheckedIOException(e.getMessage(), e);
        }
    }

    private FsDirectories newDirectories(final FileSystem pFs) {
        final FsDirectories fsdirs = fsDirectoriesFactory.newDirectories(
                newRegistrar(pFs));
        roots.add(fsdirs);
        return fsdirs;
    }

    public void addRoot(final Object pWatchedDirectoryKey, final Path pDirectory) throws IOException {
        try {
            children.computeIfAbsent(pDirectory.getFileSystem(),
                    this::newDirectories).rootAdded(pWatchedDirectoryKey, pDirectory, observers);
        } catch (final UncheckedIOException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    public void removeRoot(final Path pDirectory) {
        final FsDirectories fsdirs = children.get(pDirectory.getFileSystem());
        if (null != fsdirs && fsdirs.directoryDeleted(pDirectory)) {
            children.remove(pDirectory.getFileSystem());
        }
    }

    private FsDirectories getFsDirectories(final Path pPath) {
        final FsDirectories fsdirs = children.get(pPath.getFileSystem());
        if (null == fsdirs) {
            throw new IllegalStateException(format("No appropriate root-directory found for %s", pPath));
        }
        return fsdirs;
    }

    void pathModified(final Path pPath) {
        if (isDirectory(pPath)) {
            getFsDirectories(pPath).directoryCreated(pPath, observers);
        } else {
            getFsDirectories(pPath).getDirectory(pPath).informIfChanged(observers, pPath);
        }
    }

    void pathDeleted(final Path pPath) {
        final FsDirectories fsdirs = getFsDirectories(pPath);
        final FsBaseDirectory fsdir = fsdirs.getDirectory(pPath);

        if (fsdirs.directoryDeleted(pPath)) {
            children.remove(pPath.getFileSystem());
        }

        final FileKey key = fsdir.newKey(pPath);
        observers.forEach(o -> executorServices.getObserverExecutor().execute(
                () -> o.discard(key)));
    }

    // Lifecycle method for Felix DM
    public void stop() {
        children.values().forEach(FsDirectories::close);
        children.clear();
    }

    void close(final FsDirectories pFsDirectories) {
        if (null != pFsDirectories) {
            pFsDirectories.close();

            // Remove closed watch-service from the roots-list and children-map
            roots.remove(pFsDirectories);
            children.values().remove(pFsDirectories);
        }
    }

    public List<FsDirectories> getRoots() {
        return roots;
    }
}
