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

import ch.sourcepond.io.fileobserver.impl.observer.ObserverHandler;
import ch.sourcepond.io.fileobserver.impl.registrar.Registrar;
import org.slf4j.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.WatchKey;

import static java.lang.String.format;
import static org.slf4j.LoggerFactory.getLogger;

/**
 *
 */
public class FsDirectories implements Closeable {
    private static final Logger LOG = getLogger(FsDirectories.class);
    private final Registrar registrar;

    FsDirectories(final Registrar pRegistrar) {
        registrar = pRegistrar;
    }

    public void initiallyInformHandler(final ObserverHandler pHandler) {
        registrar.initiallyInformHandler(pHandler);
    }

    public void rootAdded(final Enum<?> pWatchedDirectoryKeyOrNull, final Path pDirectory, final ObserverHandler pHandler) {
        registrar.rootAdded(pWatchedDirectoryKeyOrNull, pDirectory, pHandler);
    }

    public void directoryCreated(final Path pDirectory, final ObserverHandler pHandler) {
        registrar.directoryCreated(pDirectory, pHandler);
    }

    public boolean directoryDeleted(final Path pDirectory) {
        return registrar.directoryDeleted(pDirectory);
    }

    public FsBaseDirectory getDirectory(final Path pFile) {
        final FsBaseDirectory dir = registrar.get(pFile.getParent());
        if (null == dir) {
            throw new NullPointerException(format("No directory object found for file %s", pFile));
        }
        return dir;
    }

    @Override
    public void close() {
        try {
            registrar.close();
        } catch (final IOException e) {
            LOG.warn(e.getMessage(), e);
        }
    }

    public WatchKey poll() {
        return registrar.poll();
    }
}
