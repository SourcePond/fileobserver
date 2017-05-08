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
package ch.sourcepond.io.fileobserver.impl.pending;

import java.nio.file.FileSystem;
import java.nio.file.WatchEvent;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static java.lang.Thread.currentThread;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.time.Instant.now;

/**
 *
 */
public class PendingEventRegistry {
    private final Map<FileSystem, Instant> pending = new HashMap<>();
    private volatile long modificationLockingTime;

    public void setModificationLockingTime(final long pModificationLockingTime) {
        modificationLockingTime = pModificationLockingTime;
    }

    private void awaitPending(final FileSystem pFs) {
        try {
            while (pending.containsKey(pFs)) {
                wait();
            }
        } catch (final InterruptedException e) {
            currentThread().interrupt();
        }
    }

    public synchronized boolean awaitIfPending(final FileSystem pFs, final WatchEvent.Kind<?> pKind) {
        boolean furtherProcessingAllowed = true;
        if (ENTRY_CREATE.equals(pKind)) {
            pending.put(pFs, now());
        } else {
            final Instant creationTime = pending.get(pFs);
            furtherProcessingAllowed = creationTime == null || now().compareTo(creationTime.plusMillis(modificationLockingTime)) > 0;
            if (furtherProcessingAllowed) {
                awaitPending(pFs);
            }
        }
        return furtherProcessingAllowed;
    }

    public synchronized void done(final FileSystem pFs) {
        pending.remove(pFs);
        notifyAll();
    }
}
