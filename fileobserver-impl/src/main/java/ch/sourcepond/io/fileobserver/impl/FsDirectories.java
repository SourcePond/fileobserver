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

import org.slf4j.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.WatchKey;
import java.util.Iterator;
import java.util.Map;

import static java.lang.String.format;
import static java.nio.file.Files.list;
import static org.slf4j.LoggerFactory.getLogger;

/**
 *
 */
class FsDirectories implements Closeable {
    private static final Logger LOG = getLogger(FsDirectories.class);
    private final Registrar registrar;

    FsDirectories(final Registrar pRegistrar) {
        registrar = pRegistrar;
    }

    void initialyInformHandler(final ObserverHandler pHandler) {
        for (final FsDirectory fsdir : registrar) {
            try {
                list(fsdir.getPath()).forEach(dir -> {
                    directoryCreated(dir, pHandler);
                });
            } catch (final IOException e) {
                LOG.warn(e.getMessage(), e);
            }
        }
    }

    void directoryCreated(final Path pDirectory, final ObserverHandler pHandler) {
        try {
            registrar.directoryCreated(pDirectory, pHandler);
        } catch (final IOException e) {
            LOG.warn(e.getMessage(), e);
        }
    }

    boolean directoryDeleted(final Path pDirectory) {
        final FsDirectory dir = registrar.remove(pDirectory);
        if (null != dir) {
            dir.cancelKey();
            for (final Iterator<Map.Entry<Path, FsDirectory>> it = registrar.entrySet().iterator() ; it.hasNext() ; ) {
                final Map.Entry<Path, FsDirectory> entry = it.next();
                if (entry.getKey().startsWith(pDirectory)) {
                    entry.getValue().cancelKey();
                    it.remove();
                }
            }
        }
        return registrar.isEmpty();
    }

    boolean isEmpty() {
        return registrar.isEmpty();
    }

    FsDirectory getDirectory(final Path pFile) {
        final FsDirectory dir = registrar.get(pFile.getParent());
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

    WatchKey poll() {
        return registrar.poll();
    }
}
