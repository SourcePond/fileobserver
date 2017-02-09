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

import ch.sourcepond.io.fileobserver.api.FileObserver;

import java.nio.file.Path;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 *
 */
class CompoundObserverHandler implements ObserverHandler {
    private final ConcurrentMap<FileObserver, ObserverHandler> handlers = new ConcurrentHashMap<>();
    private final DefaultObserverHandlerFactory handlerFactory;

    CompoundObserverHandler(final DefaultObserverHandlerFactory pHandlerFactory) {
        handlerFactory = pHandlerFactory;
    }

    void putIfAbsent(final FileObserver pObserver, final Collection<FsDirectories> pFsdirs) {
        final ObserverHandler handler = handlerFactory.newHander(pObserver);
        if (null == handlers.putIfAbsent(pObserver, handler)) {
            pFsdirs.forEach(f -> f.initialyInformHandler(handler));
        }
    }

    @Override
    public void modified(final String pId, final Path pFile) {
        handlers.values().forEach(h -> h.modified(pId, pFile));
    }

    @Override
    public void deleted(final String pId) {
        handlers.values().forEach(h -> h.deleted(pId));
    }

    void remove(final FileObserver pObserver) {
        handlers.remove(pObserver);
    }
}
