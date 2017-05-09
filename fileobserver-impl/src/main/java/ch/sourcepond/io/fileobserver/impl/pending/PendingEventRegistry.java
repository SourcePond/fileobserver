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

import org.slf4j.Logger;

import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static java.lang.Thread.currentThread;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.time.Instant.now;
import static org.slf4j.LoggerFactory.getLogger;

/**
 *
 */
public class PendingEventRegistry {
    private static final Logger LOG = getLogger(PendingEventRegistry.class);
    public static final PendingEventDone EMPTY_CALLBACK = () -> {};
    private final Map<Path, Instant> pending = new HashMap<>();
    private volatile long modificationLockingTime;

    public void setModificationLockingTime(final long pModificationLockingTime) {
        modificationLockingTime = pModificationLockingTime;
    }

    private void awaitPending(final Path pPath) {
        try {
            while (pending.containsKey(pPath)) {
                wait();
            }
        } catch (final InterruptedException e) {
            currentThread().interrupt();
        }
    }

    public synchronized boolean awaitIfPending(final Path pPath, final WatchEvent.Kind<?> pKind) {
        boolean furtherProcessingAllowed = true;
        if (ENTRY_CREATE.equals(pKind)) {
            pending.put(pPath, now());
            LOG.debug("Registered pending CREATE for {}", pPath);
        } else if (ENTRY_MODIFY.equals(pKind)) {
            final Instant creationTime = pending.get(pPath);
            furtherProcessingAllowed = creationTime == null || now().compareTo(creationTime.plusMillis(modificationLockingTime)) > 0;
            if (furtherProcessingAllowed) {
                LOG.debug("Waiting until MODIFY can be processed, detected pending CREATE for {}", pPath);
                awaitPending(pPath);
                LOG.debug("Pending CREATE processed, going on with MODIFY for {}", pPath);
            } else {
                LOG.debug("MODIFY detected within modification locking time of {} ms, event dropped for {}", modificationLockingTime, pPath);
            }
        }
        return furtherProcessingAllowed;
    }

    public synchronized void done(final Path pPath) {
        pending.remove(pPath);
        notifyAll();
    }
}
