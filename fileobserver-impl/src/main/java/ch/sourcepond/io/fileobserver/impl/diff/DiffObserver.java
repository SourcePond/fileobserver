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
package ch.sourcepond.io.fileobserver.impl.diff;

import ch.sourcepond.io.checksum.api.Resource;
import ch.sourcepond.io.checksum.api.Update;
import ch.sourcepond.io.fileobserver.api.FileKey;
import ch.sourcepond.io.fileobserver.api.FileObserver;
import ch.sourcepond.io.fileobserver.impl.Config;
import ch.sourcepond.io.fileobserver.impl.directory.Directory;
import ch.sourcepond.io.fileobserver.impl.fs.DedicatedFileSystem;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Executor;

import static org.slf4j.LoggerFactory.getLogger;

/**
 *
 */
public class DiffObserver implements FileObserver {
    private static final Logger LOG = getLogger(DiffObserver.class);
    private final Map<FileKey, Path> modifiedKeys = new HashMap<>();
    private final Set<FileKey> discardedKeys = new HashSet<>();
    private final Map<FileKey, Collection<FileKey>> supplementKeys = new HashMap<>();
    private final DedicatedFileSystem fs;
    private final Executor observerExecutor;
    private final Collection<FileObserver> delegates;
    private final Config config;

    DiffObserver(final DedicatedFileSystem pFs,
                 final Executor pObserverExecutor,
                 final Collection<FileObserver> pDelegates,
                 final Config pConfig) {
        fs = pFs;
        observerExecutor = pObserverExecutor;
        delegates = pDelegates;
        config = pConfig;
    }

    private void informDiscard(final FileKey pKey) {
        for (final FileObserver delegate : delegates) {
            observerExecutor.execute(() -> delegate.discard(pKey));
        }
    }

    private void informDelegate(final FileObserver pDelegate, final FileKey pKey, final Path pFile) {
        try {
            final Collection<FileKey> supplementKeysOrNull = supplementKeys.get(pKey);
            if (supplementKeysOrNull != null) {
                for (final FileKey supplementKey : supplementKeysOrNull) {
                    pDelegate.supplement(pKey, supplementKey);
                }
            }
            pDelegate.modified(pKey, pFile);
        } catch (final IOException e) {
            LOG.warn(e.getMessage(), e);
        }
    }

    private void informModified(final Update pUpdate, final FileKey pKey, final Path pFile) {
        if (pUpdate.hasChanged()) {
            for (final FileObserver delegate : delegates) {
                observerExecutor.execute(() -> informDelegate(delegate, pKey, pFile));
            }
        }
    }

    private Resource getResource(final Path pFile) {
        final Directory dir = fs.getDirectory(pFile.getParent());
        if (dir == null) {
            LOG.warn("Checksum update cancelled because no directory registered for {}", pFile);
            return null;
        }
        return dir.getResource(pFile);
    }

    private void updateResource(final FileKey pKey, final Path pFile) {
        final Resource resource = getResource(pFile);
        if (resource != null) {
            try {
                resource.update(config.timeout(), u -> informModified(u, pKey, pFile));
            } catch (final IOException e) {
                LOG.warn(e.getMessage(), e);
            }
        }
    }

    public void finalizeRelocation() throws IOException {
        modifiedKeys.forEach(this::updateResource);
        discardedKeys.removeAll(modifiedKeys.keySet());
        discardedKeys.forEach(this::informDiscard);
    }

    @Override
    public void modified(final FileKey pKey, final Path pFile) throws IOException {
        modifiedKeys.put(pKey, pFile);
    }

    @Override
    public void discard(final FileKey pKey) {
        discardedKeys.add(pKey);
    }

    @Override
    public void supplement(final FileKey pKnownKey, final FileKey pAdditionalKey) {
        supplementKeys.computeIfAbsent(pKnownKey, k -> new LinkedHashSet<FileKey>()).add(pAdditionalKey);
    }
}
