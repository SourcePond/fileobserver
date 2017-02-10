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
package ch.sourcepond.io.fileobserver.impl.observer;

import ch.sourcepond.io.fileobserver.api.FileKey;
import ch.sourcepond.io.fileobserver.api.FileObserver;

import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

/**
 *
 */
class DefaultObserverHandler implements ObserverHandler {
    private final Set<FileKey> acceptedIds = ConcurrentHashMap.newKeySet();
    private final ExecutorService observerExecutor;
    private final FileObserver delegate;

    DefaultObserverHandler(final ExecutorService pObserverExecutor, final FileObserver pDelegate) {
        observerExecutor = pObserverExecutor;
        delegate = pDelegate;
    }

    private boolean accept(final FileKey pKey, final Path pFile) {
        final boolean accepted = delegate.accept(pKey, pFile);
        if (accepted) {
            acceptedIds.add(pKey);
        }
        return accepted;
    }

    @Override
    public void modified(final FileKey pKey, final Path pFile) {
        observerExecutor.execute(() -> {
            if (accept(pKey, pFile)) {
                delegate.modified(pKey, pFile);
            }
        });
    }

    @Override
    public void deleted(final FileKey pKey) {
        observerExecutor.execute(() -> {
            if (acceptedIds.remove(pKey)) {
                delegate.deleted(pKey);
            }
        });
    }
}
