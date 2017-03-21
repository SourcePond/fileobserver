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
import ch.sourcepond.io.fileobserver.impl.filekey.DefaultFileKeyFactory;
import ch.sourcepond.io.fileobserver.impl.fs.DedicatedFileSystem;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutorService;

import static ch.sourcepond.io.fileobserver.impl.directory.Directory.TIMEOUT;
import static java.nio.file.Files.walkFileTree;
import static org.slf4j.LoggerFactory.getLogger;

/**
 *
 */
public class DiffObserver implements FileObserver {
    private static final Logger LOG = getLogger(DiffObserver.class);
    private final Map<FileKey, Path> modifiedKeys = new HashMap<>();
    private final Set<FileKey> discardedKeys = new HashSet<>();
    private final Map<FileKey, Collection<FileKey>> supplementKeys = new HashMap<>();
    private final DefaultFileKeyFactory keyFactory;
    private final Object directoryKey;
    private final DedicatedFileSystem fs;
    private final ExecutorService observerExecutor;
    private final Collection<FileObserver> delegates;
    private final Path previousDirectory;

    DiffObserver(final DefaultFileKeyFactory pKeyFactory,
                 final Object pDirectoryKey,
                 final DedicatedFileSystem pFs,
                 final ExecutorService pObserverExecutor,
                 final Collection<FileObserver> pDelegates,
                 final Path pPreviousWatchedDirectory) {
        keyFactory = pKeyFactory;
        directoryKey = pDirectoryKey;
        fs = pFs;
        observerExecutor = pObserverExecutor;
        delegates = pDelegates;
        previousDirectory = pPreviousWatchedDirectory;
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
        return fs.getDirectory(pFile.getParent()).getResource(pFile);
    }

    private void updateResource(final FileKey pKey, final Path pFile) {
        try {
            getResource(pFile).update(TIMEOUT, u -> informModified(u, pKey, pFile));
        } catch (final IOException e) {
            LOG.warn(e.getMessage(), e);
        }
    }

    public void finalizeRelocation() throws IOException {
        final DirectoryContent previousContent =new DirectoryContent(keyFactory, directoryKey, previousDirectory);
        walkFileTree(previousDirectory, previousContent);
        modifiedKeys.forEach(this::updateResource);
        final Set<FileKey> previousKeys = previousContent.getKeys();
        previousKeys.removeAll(modifiedKeys.entrySet());
        previousKeys.forEach(this::informDiscard);
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
        supplementKeys.computeIfAbsent(pKnownKey, k -> new HashSet<FileKey>()).add(pAdditionalKey);
    }
}
