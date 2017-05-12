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

import ch.sourcepond.io.fileobserver.spi.WatchedDirectory;

import java.nio.file.Path;
import java.nio.file.WatchKey;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.CopyOnWriteArraySet;

import static java.util.Objects.requireNonNull;

/**
 *
 */
public class SubDirectory extends Directory {
    private volatile Directory parent;
    private volatile Collection<WatchedDirectory> watchedDirectoriesOrNull;

    SubDirectory(final Directory pParent, final WatchKey pWatchKey) {
        this(pParent, pWatchKey, null);
    }

    /**
     * This constructor is used by {@link RootDirectory#rebase(Directory)}.
     *
     * @param pParent
     * @param pWatchKey
     * @param pWatchedDirectoriesOrNull
     */
    SubDirectory(final Directory pParent,
                 final WatchKey pWatchKey,
                 final Collection<WatchedDirectory> pWatchedDirectoriesOrNull) {
        super(pWatchKey);
        parent = requireNonNull(pParent,"Parent is null");;
        watchedDirectoriesOrNull = pWatchedDirectoriesOrNull;
    }

    @Override
    DirectoryFactory getFactory() {
        return parent.getFactory();
    }

    @Override
    public void addWatchedDirectory(final WatchedDirectory pDirectoryKey) {
        Collection<WatchedDirectory> dirs = watchedDirectoriesOrNull;
        if (dirs == null) {
            synchronized (this) {
                if (watchedDirectoriesOrNull == null) {
                    dirs = watchedDirectoriesOrNull = new CopyOnWriteArraySet<>();
                }
            }
        }
        dirs.add(pDirectoryKey);
    }

    @Override
    public boolean remove(final WatchedDirectory pDirectoryKey) {
        boolean rc = false;
        final Collection<WatchedDirectory> keys = watchedDirectoriesOrNull;
        if (keys != null) {
            rc = keys.remove(pDirectoryKey);
            if (keys.isEmpty()) {
                watchedDirectoriesOrNull = null;
            }
        }
        return rc;
    }

    @Override
    Collection<WatchedDirectory> getWatchedDirectories() {
        final Collection<WatchedDirectory> dirs;
        Collection<WatchedDirectory> dirsOrNull = watchedDirectoriesOrNull;
        if (dirsOrNull == null) {
            dirs = parent.getWatchedDirectories();
        } else {
            dirs = new HashSet<>(parent.getWatchedDirectories());
            dirs.addAll(dirsOrNull);
        }
        return dirs;
    }

    @Override
    Path relativizeAgainstRoot(final WatchedDirectory pWatchedDirectory, final Path pPath) {
        final Path relativePath;
        final Collection<WatchedDirectory> dirs = watchedDirectoriesOrNull;
        if (dirs != null && dirs.contains(pWatchedDirectory)) {
            relativePath = getPath().relativize(pPath);
        } else {
            relativePath = parent.relativizeAgainstRoot(pWatchedDirectory, pPath);
        }
        return relativePath;
    }

    @Override
    public boolean isRoot() {
        return false;
    }

    @Override
    public boolean hasKeys() {
        final Collection<WatchedDirectory> keys = watchedDirectoriesOrNull;
        return keys != null && !keys.isEmpty();
    }

    @Override
    public Directory rebase(final Directory pBaseDirectory) {
        parent = pBaseDirectory;
        return this;
    }

    @Override
    public Directory toRootDirectory() {
        return new RootDirectory(getFactory(), getWatchKey(), watchedDirectoriesOrNull);
    }

    public Directory getParent() {
        return parent;
    }

    @Override
    long getTimeout() {
        return parent.getTimeout();
    }
}
