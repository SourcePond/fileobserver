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
import static org.slf4j.LoggerFactory.getLogger;

/**
 *
 */
public abstract class FsBaseDirectory {
    private static final Logger LOG = getLogger(FsDirectory.class);
    static final long TIMEOUT = 2000;
    private final ConcurrentMap<Path, Resource> resources = new ConcurrentHashMap<>();

    abstract WatchKey getWatchKey();

    /**
     * Returns the key of the watched directory i.e. the key of the root-directory (specified
     * by {@link ch.sourcepond.io.fileobserver.spi.WatchedDirectory})
     *
     * @return Watched directory key, never {@code null}
     */
    abstract Enum<?> getWatchedDirectoryKey();

    Path getPath() {
        return (Path) getWatchKey().watchable();
    }

    abstract Resource newResource(Algorithm pAlgorithm, Path pFile);

    public abstract FileKey newKey(Path pFile);

    public void cancelKey() {
        getWatchKey().cancel();
    }

    public void forceInformAboutAllDirectChildFiles(final Collection<FileObserver> pObservers) {
        try (final DirectoryStream<Path> stream = newDirectoryStream(getPath(), Files::isRegularFile)) {
            stream.forEach(f -> forceInform(pObservers, f));
        } catch (final IOException e) {
            LOG.warn(e.getMessage(), e);
        }
    }

    public void forceInform(final Collection<FileObserver> pObservers, final Path pFile) {
        final FileKey key = newKey(pFile);
        pObservers.forEach(o -> o.modified(key, pFile));
    }

    void informIfChanged(final Collection<FileObserver> pObservers, final Path pFile) {
        // TODO: Replace interval with configurable value
        resources.computeIfAbsent(pFile,
                f -> newResource(SHA256, pFile)).update(TIMEOUT,
                (pPrevious, pCurrent) -> informObservers(pPrevious, pCurrent, pObservers, pFile));
    }

    private void informObservers(final Checksum pPrevious, final Checksum pCurrent, final Collection<FileObserver> pObservers, final Path pFile) {
        if (!pPrevious.equals(pCurrent)) {
            final FileKey key = newKey(pFile);
            pObservers.forEach(o -> o.modified(key, pFile));
        }
    }
}
