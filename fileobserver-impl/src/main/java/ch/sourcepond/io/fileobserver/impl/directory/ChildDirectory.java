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
import ch.sourcepond.io.checksum.api.Checksum;
import ch.sourcepond.io.checksum.api.Resource;
import ch.sourcepond.io.fileobserver.api.FileKey;
import ch.sourcepond.io.fileobserver.api.FileObserver;

import java.nio.file.Path;
import java.nio.file.WatchKey;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 *
 */
class ChildDirectory extends Directory {
    private final Directory parent;
    private final WatchKey watchKey;
    private volatile Set<Object> directoryKeysOrNull;

    ChildDirectory(final Directory pParent, final WatchKey pWatchKey) {
        parent = pParent;
        watchKey = pWatchKey;
    }

    @Override
    public void addDirectoryKey(final Object pKey) {
        if (directoryKeysOrNull == null) {
            synchronized (this) {
                if (directoryKeysOrNull == null) {
                    directoryKeysOrNull = new CopyOnWriteArraySet<>();
                }
            }
        }
        directoryKeysOrNull.add(pKey);
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
    WatchKey getWatchKey() {
        return watchKey;
    }

    @Override
    Resource newResource(final Algorithm pAlgorithm, final Path pFile) {
        return parent.newResource(pAlgorithm, pFile);
    }

    @Override
    public Collection<FileKey> createKeys(final Path pFile) {
        return parent.createKeys(pFile);
    }

    @Override
    void informObservers(final Checksum pPrevious, final Checksum pCurrent, final Collection<FileObserver> pObservers, final Path pFile) {
        parent.informObservers(pPrevious, pCurrent, pObservers, pFile);
    }

    @Override
    public void forceInformObservers(final Collection<FileObserver> pObservers, final Path pFile) {
        parent.forceInformObservers(pObservers, pFile);
    }
}
