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

import ch.sourcepond.io.fileobserver.api.ChangeEvent;
import ch.sourcepond.io.fileobserver.api.DispatchKey;
import ch.sourcepond.io.fileobserver.api.PathChangeListener;

import java.nio.file.Path;
import java.util.Collection;

/**
 *
 */
class DefaultChangeEvent implements ChangeEvent {
    private final DispatchKey key;
    private final Path file;
    private final Collection<DispatchKey> parentKeys;
    private final PathChangeListener listener;
    private final ReplayDispatcher replayDispatcher;
    private volatile int numReplays;

    DefaultChangeEvent(final PathChangeListener pListener,
                       final DispatchKey pKey,
                       final Path pFile,
                       final Collection<DispatchKey> pParentKeys,
                       final ReplayDispatcher pReplayDispatcher) {
        listener = pListener;
        key = pKey;
        file = pFile;
        parentKeys = pParentKeys;
        replayDispatcher = pReplayDispatcher;
    }

    @Override
    public DispatchKey getKey() {
        return key;
    }

    @Override
    public Path getFile() {
        return file;
    }

    @Override
    public int getNumReplays() {
        return numReplays;
    }

    @Override
    public void replay() {
        numReplays++;
        replayDispatcher.replay(listener, this, parentKeys);
    }

    @Override
    public String toString() {
        return "ChangeEvent[key: " + key + ", numReplays: " + numReplays + ", file: " + file + "]";
    }
}
