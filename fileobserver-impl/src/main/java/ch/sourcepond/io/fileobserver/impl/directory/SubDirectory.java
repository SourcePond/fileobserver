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

import java.nio.file.Path;
import java.nio.file.WatchKey;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 *
 */
class SubDirectory extends Directory {
    private volatile Directory parent;
    private volatile Collection<Object> directoryKeysOrNull;

    SubDirectory(final Directory pParent, final WatchKey pWatchKey) {
        super(pWatchKey);
        parent = pParent;
    }

    /**
     * This constructor is used by {@link RootDirectory#rebase(Directory)}.
     *
     * @param pParent
     * @param pWatchKey
     * @param pDirectoryKeysOrNull
     */
    SubDirectory(final Directory pParent,
                         final WatchKey pWatchKey,
                         final Collection<Object> pDirectoryKeysOrNull) {
        super(pWatchKey);
        parent = pParent;
        directoryKeysOrNull = pDirectoryKeysOrNull;
    }

    @Override
    DirectoryFactory getFactory() {
        return parent.getFactory();
    }

    @Override
    public void addDirectoryKey(final Object pDirectoryKey) {
        Collection<Object> keys = directoryKeysOrNull;
        if (keys == null) {
            synchronized (this) {
                if (directoryKeysOrNull == null) {
                    keys = directoryKeysOrNull = new CopyOnWriteArraySet<>();
                }
            }
        }
        keys.add(pDirectoryKey);
    }

    @Override
    public boolean removeDirectoryKey(final Object pDirectoryKey) {
        final Collection<Object> keys = directoryKeysOrNull;
        boolean rc = false;
        if (keys != null) {
            rc = keys.remove(pDirectoryKey);
            if (keys.isEmpty()) {
                synchronized (this) {
                    directoryKeysOrNull = null;
                }
            }
        }
        return rc;
    }

    @Override
    Collection<Object> getDirectoryKeys() {
        final Collection<Object> keys;
        Collection<Object> keysOrNull = directoryKeysOrNull;
        if (keysOrNull == null) {
            keys = parent.getDirectoryKeys();
        } else {
            keys = new HashSet<>(parent.getDirectoryKeys());
            keys.addAll(keysOrNull);
        }
        return keys;
    }

    @Override
    Path relativizeAgainstRoot(final Object pDirectoryKey, final Path pPath) {
        final Path relativePath;
        final Collection<Object> keys = directoryKeysOrNull;
        if (keys != null && keys.contains(pDirectoryKey)) {
            relativePath = getPath().relativize(pPath);
        } else {
            relativePath = parent.relativizeAgainstRoot(pDirectoryKey, pPath);
        }
        return relativePath;
    }

    @Override
    public boolean isRoot() {
        return false;
    }

    @Override
    public boolean hasKeys() {
        final Collection<Object> keys = directoryKeysOrNull;
        return keys != null && !keys.isEmpty();
    }

    @Override
    public Directory rebase(final Directory pBaseDirectory) {
        parent = pBaseDirectory;
        return this;
    }

    @Override
    public Directory toRootDirectory() {
        return new RootDirectory(getFactory(), getWatchKey(), directoryKeysOrNull);
    }
}
