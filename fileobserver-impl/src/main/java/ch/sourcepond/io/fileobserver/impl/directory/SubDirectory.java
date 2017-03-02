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

import ch.sourcepond.io.checksum.api.Algorithm;
import ch.sourcepond.io.checksum.api.Resource;
import ch.sourcepond.io.fileobserver.api.FileKey;

import java.nio.file.Path;
import java.nio.file.WatchKey;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 *
 */
class SubDirectory extends Directory {
    private final Directory parent;
    private volatile Collection<Object> directoryKeysOrNull;

    SubDirectory(final Directory pParent, final WatchKey pWatchKey) {
        super(pWatchKey);
        parent = pParent;
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
            keys.remove(pDirectoryKey);
            rc = keys.isEmpty();
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
    FileKey createKey(final Object pDirectoryKey, final Path pRelativePath) {
        return parent.createKey(pDirectoryKey, pRelativePath);
    }

    @Override
    Resource createResource(final Algorithm pAlgorithm, final Path pFile) {
        return parent.createResource(pAlgorithm, pFile);
    }

    @Override
    void execute(final Runnable pTask) {
        parent.execute(pTask);
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
}
