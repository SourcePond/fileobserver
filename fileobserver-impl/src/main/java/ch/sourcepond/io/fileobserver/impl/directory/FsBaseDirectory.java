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
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchKey;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static ch.sourcepond.io.checksum.api.Algorithm.SHA256;
import static java.nio.file.Files.newDirectoryStream;
import static java.util.Arrays.asList;
import static org.slf4j.LoggerFactory.getLogger;

/**
 *
 */
public abstract class FsBaseDirectory {
    private static final Logger LOG = getLogger(FsDirectory.class);
    static final long TIMEOUT = 2000;
    private final ConcurrentMap<Path, Resource> resources = new ConcurrentHashMap<>();

    abstract void addDirectoryKey(Object pKey);

    abstract Collection<Object> getDirectoryKeys();

    abstract WatchKey getWatchKey();

    Path getPath() {
        return (Path) getWatchKey().watchable();
    }

    abstract Resource newResource(Algorithm pAlgorithm, Path pFile);

    public abstract Collection<FileKey> createKeys(Path pFile);

    public void cancelKey() {
        getWatchKey().cancel();
    }

    public void forceInformAboutAllDirectChildFiles(final FileObserver pObserver) {
        try (final DirectoryStream<Path> stream = newDirectoryStream(getPath(), Files::isRegularFile)) {
            stream.forEach(f -> forceInformObservers(asList(pObserver), f));
        } catch (final IOException e) {
            LOG.warn(e.getMessage(), e);
        }
    }

    public abstract void forceInformObservers(Collection<FileObserver> pObservers, Path pFile);

    void informIfChanged(final Collection<FileObserver> pObservers, final Path pFile) {
        // TODO: Replace interval with configurable value
        resources.computeIfAbsent(pFile,
                f -> newResource(SHA256, pFile)).update(TIMEOUT,
                (pPrevious, pCurrent) -> informObservers(pPrevious, pCurrent, pObservers, pFile));
    }

    abstract void informObservers(Checksum pPrevious, Checksum pCurrent, Collection<FileObserver> pObservers, Path pFile);
}
