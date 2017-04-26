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
import java.util.Collection;

import static java.util.Arrays.asList;

/**
 *
 */
public class EventDispatcher {
    private final ObserverManager dispatcher;
    private final Collection<FileObserver> observers;

    EventDispatcher(final ObserverManager pManager,
                    final FileObserver pAddedObserver) {
        this(pManager, asList(pAddedObserver));
    }

    EventDispatcher(final ObserverManager pManager,
                    final Collection<FileObserver> pObservers) {
        dispatcher = pManager;
        observers = pObservers;
    }

    public boolean hasObservers() {
        return !observers.isEmpty();
    }

    public void modified(final Collection<FileKey> pKeys, final Path pFile, final Collection<FileKey> pParentKeys) {
        dispatcher.modified(observers, pKeys, pFile, pParentKeys);
    }

    public void modified(final FileKey pKey, final Path pFile, final Collection<FileKey> pParentKeys) {
        dispatcher.modified(observers, pKey, pFile, pParentKeys);
    }

    public void discard(final FileKey pKey) {
        dispatcher.discard(observers, pKey);
    }
}
