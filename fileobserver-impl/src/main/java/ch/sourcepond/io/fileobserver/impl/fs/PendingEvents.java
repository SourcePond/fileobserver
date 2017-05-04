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
package ch.sourcepond.io.fileobserver.impl.fs;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import static java.lang.Thread.currentThread;
import static java.time.Instant.now;
import static java.util.concurrent.ConcurrentHashMap.newKeySet;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 *
 */
public class PendingEvents implements Runnable {

    private class PendingEvent implements Delayed {
        private final Instant creationTime = now();
        private final Instant threshold;
        private final Path path;

        PendingEvent(final Path pPath, final long pTimeoutInMilliseconds) {
            path = pPath;
            threshold = creationTime.plusMillis(pTimeoutInMilliseconds);
        }

        @Override
        public long getDelay(final TimeUnit unit) {
            return unit.convert(threshold.minusMillis(now().toEpochMilli()).toEpochMilli(), MILLISECONDS);
        }

        @Override
        public int compareTo(final Delayed o) {
            final long t = getDelay(MILLISECONDS);
            final long ot = o.getDelay(MILLISECONDS);

            if (t > ot) {
                return 1;
            }
            if (t < ot) {
                return -1;
            }
            return 0;
        }

        Path getPath() {
            return path;
        }
    }

    private final DelayQueue<PendingEvent> queue = new DelayQueue<>();
    private final Set<Path> pending =  newKeySet();
    private final Thread thread = new Thread(this);
    private volatile long timoutInMilliseconds;

    public void start() {
        thread.setDaemon(true);
        thread.start();
    }

    public void stop() {
        thread.interrupt();
    }

    public void setTimoutInMilliseconds(final long pTimeoutInMilliseconds) {
        timoutInMilliseconds = pTimeoutInMilliseconds;
    }

    void createEventReceived(final Path pPath) {
        if (pending.add(pPath)) {
            queue.offer(new PendingEvent(pPath, timoutInMilliseconds));
        }
    }

    boolean isModificationAllowed(final Path pPath) {
        return !pending.contains(pPath);
    }

    @Override
    public void run() {
        try {
            while (!currentThread().isInterrupted()) {
                pending.remove(queue.take().getPath());
            }
        } catch (final InterruptedException e) {
            currentThread().interrupt();
        }
    }
}
