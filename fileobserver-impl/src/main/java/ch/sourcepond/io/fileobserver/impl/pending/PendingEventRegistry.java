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

import ch.sourcepond.io.fileobserver.impl.Config;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static java.lang.String.valueOf;
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
    private volatile long modificationLockingTimeInMilliseconds;
    private volatile long pendingTimeoutInMilliseconds;

    public void setConfig(final Config pConfig) {
        modificationLockingTimeInMilliseconds = pConfig.modificationLockingMillis();
        pendingTimeoutInMilliseconds = pConfig.pendingTimeoutMillis();
    }

    private void awaitPending(final Path pPath, final WatchEvent.Kind<?> pKind) {
        try {
            final Instant start = now();
            final Instant threshold = start.plusMillis(pendingTimeoutInMilliseconds);
            while (pending.containsKey(pPath) && threshold.compareTo(now()) > 0) {
                wait(pendingTimeoutInMilliseconds);
            }

            // Wait timed out! Log warning
            if (pending.remove(pPath) != null) {
                LOG.warn("No notification received, force processing after {} ms: {}, {}", now().minusMillis(start.toEpochMilli()), pKind, pPath);
            }
        } catch (final InterruptedException e) {
            currentThread().interrupt();
        }
    }

    public synchronized boolean awaitIfPending(final Path pPath, final WatchEvent.Kind<?> pKind) {
        boolean furtherProcessingAllowed = true;
        if (ENTRY_CREATE.equals(pKind)) {
            pending.put(pPath, now());
            LOG.debug("Registered pending {} for {}", pKind, pPath);
        } else if (ENTRY_MODIFY.equals(pKind)) {
            final Instant creationTime = pending.get(pPath);
            if (creationTime != null) {
                furtherProcessingAllowed = now().compareTo(creationTime.plusMillis(modificationLockingTimeInMilliseconds)) > 0;
                if (furtherProcessingAllowed) {
                    LOG.debug("Wait until {} can be processed for {}", pKind, pPath);
                    final long start = now().toEpochMilli();
                    awaitPending(pPath, pKind);
                    final long end = now().toEpochMilli();
                    LOG.debug("Processing {} after timeout of {}, {}", pKind, valueOf(end - start), pPath);
                } else {
                    LOG.debug("Drop because modification locking time not expired, {} ", pKind, pPath);
                }
            } else {
                LOG.debug("Directly process {} for {}", pKind, pPath);
            }
        }
        return furtherProcessingAllowed;
    }

    public synchronized void done(final Path pPath) {
        pending.remove(pPath);
        notifyAll();
    }
}
