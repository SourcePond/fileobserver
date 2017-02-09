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
package ch.sourcepond.io.fileobserver.impl;

import ch.sourcepond.io.fileobserver.api.FileObserver;
import org.slf4j.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.lang.String.format;
import static java.nio.file.Files.isDirectory;
import static org.slf4j.LoggerFactory.getLogger;

/**
 *
 */
class Directories implements Closeable {
    private static final Logger LOG = getLogger(Directories.class);
    private final ConcurrentMap<FileSystem, FsDirectories> children = new ConcurrentHashMap<>();

    /**
     * We intentionally do <em>not</em> use {@code children.values()} because we need to iterate
     * over the children in the scanner-thread loop. If we used {@code children.values()} directly
     * we must create a new iterator during every loop iteration. This is a potential memory leak
     * and causes unnecessary GC activity.
     */
    private final List<FsDirectories> roots;

    private final RegistrarFactory registrarFactory;
    private final CompoundObserverHandler observers;
    private final FsDirectoriesFactory fsDirectoriesFactory;

    public Directories(final RegistrarFactory pRegistrarFactory,
                       final CompoundObserverHandler pObservers,
                       final FsDirectoriesFactory pFsDirectories,
                       final List<FsDirectories> pRoots) {
        registrarFactory = pRegistrarFactory;
        observers = pObservers;
        fsDirectoriesFactory = pFsDirectories;
        roots = pRoots;
    }

    void addObserver(final FileObserver pObserver) {
        observers.putIfAbsent(pObserver, children.values());
    }

    void removeObserver(final FileObserver pObserver) {
        observers.remove(pObserver);
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

    void addRoot(final Path pDirectory) throws IOException {
        if (!isDirectory(pDirectory)) {
            throw new IllegalArgumentException(format("%s is not a directory!", pDirectory));
        }
        try {
            children.computeIfAbsent(pDirectory.getFileSystem(),
                    fs -> newDirectories(fs)).directoryCreated(pDirectory, observers);
        } catch (final UncheckedIOException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    void removeRoot(final Path pDirectory) {
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

    private String toId(final Path pPath) {
        return getFsDirectories(pPath).getDirectory(pPath).relativize(pPath);
    }

    void pathModified(final Path pPath) {
        if (isDirectory(pPath)) {
            getFsDirectories(pPath).directoryCreated(pPath, observers);
        } else {
            final FsDirectory dir = getFsDirectories(pPath).getDirectory(pPath);
            dir.informIfChanged(observers, pPath);
        }
    }

    void pathDeleted(final Path pPath) {
        final FsDirectories fsdirs = getFsDirectories(pPath);
        final String id = toId(pPath);
        if (fsdirs.directoryDeleted(pPath)) {
            children.remove(pPath.getFileSystem());
        }
        observers.deleted(id);
    }

    @Override
    public void close() {
        for (final FsDirectories fsdirs : children.values()) {
            fsdirs.close();
        }
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
}
