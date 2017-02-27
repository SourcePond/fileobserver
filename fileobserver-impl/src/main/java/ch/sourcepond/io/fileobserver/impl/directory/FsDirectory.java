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

/**
 *
 */
public class FsDirectory extends FsBaseDirectory {
    private final FsBaseDirectory parent;
    private final WatchKey watchKey;

    FsDirectory(final FsBaseDirectory pParent, final WatchKey pWatchKey) {
        parent = pParent;
        watchKey = pWatchKey;
    }

    @Override
    WatchKey getWatchKey() {
        return watchKey;
    }

    @Override
    Object getWatchedDirectoryKey() {
        return parent.getWatchedDirectoryKey();
    }

    @Override
    Resource newResource(final Algorithm pAlgorithm, final Path pFile) {
        return parent.newResource(pAlgorithm, pFile);
    }

    @Override
    public FileKey newKey(final Path pFile) {
        return parent.newKey(pFile);
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
