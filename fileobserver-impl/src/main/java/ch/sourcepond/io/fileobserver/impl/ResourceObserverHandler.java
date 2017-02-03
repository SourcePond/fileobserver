package ch.sourcepond.io.fileobserver.impl;

import ch.sourcepond.io.fileobserver.api.ResourceObserver;

import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 */
class ResourceObserverHandler {
    private final Set<String> acceptedIds = ConcurrentHashMap.newKeySet();
    private final ResourceObserver delegate;

    ResourceObserverHandler(final ResourceObserver pDelegate) {
        delegate = pDelegate;
    }

    private boolean accept(final String pId, final Path pFile) {
        if (delegate.accept(pId, pFile)) {
            acceptedIds.add(pId);
            return true;
        }
        return false;
    }

    void modified(final String pId, final Path pFile) {
        if (accept(pId, pFile)) {
            delegate.modified(pId, pFile);
        }
    }

    void deleted(final String pId) {
        if (acceptedIds.remove(pId)) {
            delegate.deleted(pId);
        }
    }
}
