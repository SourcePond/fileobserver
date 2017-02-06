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

import ch.sourcepond.io.checksum.api.Checksum;
import ch.sourcepond.io.checksum.api.Resource;
import ch.sourcepond.io.checksum.api.ResourcesFactory;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.nio.file.WatchKey;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static ch.sourcepond.io.checksum.api.Algorithm.SHA256;
import static org.slf4j.LoggerFactory.getLogger;

/**
 *
 */
class FsDirectory  {
    private static final Logger LOG = getLogger(FsDirectory.class);
    private final ConcurrentMap<Path, Resource> resources = new ConcurrentHashMap<>();
    private final ResourcesFactory resourcesFactory;
    private final FsDirectory parent;
    private final WatchKey key;

    FsDirectory(final ResourcesFactory pResourcesFactory, final FsDirectory pParent, final WatchKey pKey) {
        resourcesFactory = pResourcesFactory;
        parent = pParent;
        key = pKey;
    }

    Path getPath() {
        return (Path)key.watchable();
    }

    private Path findRoot() {
        Path rootDir = getPath();
        FsDirectory dir = parent;
        while (null != dir) {
            rootDir = dir.getPath();
            dir = dir.parent;
        }
        return rootDir;
    }

    String relativize(final Path pPath) {
        return findRoot().relativize(pPath).toString();
    }

    void cancelKey() {
        key.cancel();
    }

    public void informIfChanged(final ObserverHandler pObserver, final Path pFile) {
        // TODO: Replace interval with configurable value
        resources.computeIfAbsent(pFile,
                f -> resourcesFactory.create(SHA256, pFile)).update(2000, (
                        (pPrevious, pCurrent) -> informObservers(pPrevious, pCurrent, pObserver, pFile)));
    }

    private void informObservers(final Checksum pPrevious, final Checksum pCurrent, final ObserverHandler pObserver, final Path pFile) {
        if (!pPrevious.equals(pCurrent)) {
            pObserver.modified(relativize(pFile), pFile);
        }
    }
}
