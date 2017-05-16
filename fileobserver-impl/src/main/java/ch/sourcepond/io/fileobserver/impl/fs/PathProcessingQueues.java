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

import org.slf4j.Logger;

import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.time.Instant;
import java.util.Queue;
import java.util.concurrent.*;

import static java.lang.Thread.currentThread;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.time.Instant.now;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.slf4j.LoggerFactory.getLogger;

/**
 *
 */
public class PathProcessingQueues implements Runnable {
    private static final Logger LOG = getLogger(PathProcessingQueues.class);

    private static class DeleteDelay implements Delayed {
        private final Instant creationTime = now();
        private final Instant timeout;
        private final PathQueue queue;

        DeleteDelay(final PathQueue pQueue, final long pTimeoutMillis) {
            queue = pQueue;
            timeout = creationTime.plusMillis(pTimeoutMillis);
        }

        void addDeleteEvent() {
            queue.addEvent(ENTRY_DELETE, false);
        }

        @Override
        public long getDelay(final TimeUnit unit) {
            final long remainingMillis = timeout.minusMillis(now().toEpochMilli()).toEpochMilli();
            return unit.convert(remainingMillis, MILLISECONDS);
        }

        @Override
        public int compareTo(final Delayed o) {
            return timeout.compareTo(((DeleteDelay)o).timeout);
        }
    }

    /**
     *
     */
    private class PathQueue implements Runnable {
        private final Queue<Runnable> taskQueue = new LinkedBlockingQueue<>();
        private final Path path;
        private final PathProcessingHandler handler;
        private DeleteDelay delay;

        PathQueue(final Path pPath, final PathProcessingHandler pHandler) {
            path = pPath;
            handler = pHandler;
        }

        Path getPath() {
            return path;
        }

        synchronized void addEvent(final WatchEvent.Kind<?> pKind, boolean pFirstDelivery) {
            if (ENTRY_DELETE.equals(pKind) && pFirstDelivery) {
                delay = new DeleteDelay(this, reCreateTimeout);
                delayedDeletes.offer(delay);
            } else {
                final WatchEvent.Kind<?> kind;
                if (delay != null) {
                    delayedDeletes.remove(delay);
                    delay = null;
                    if (ENTRY_DELETE.equals(pKind)) {
                        kind = ENTRY_DELETE;
                        LOG.debug("Delivering delayed {} event for path {}", ENTRY_DELETE, path);
                    } else {
                        kind = ENTRY_MODIFY;
                        LOG.debug("Detected re-creation; delivering {} instead of {} for path {}", kind, pKind, path);
                    }
                } else {
                    kind = pKind;
                    LOG.debug("Delivering event {} as-it-is for {}", kind, path);
                }

                if (taskQueue.isEmpty()) {
                    handler.process(kind, path, this);
                } else {
                    taskQueue.offer(() -> handler.process(kind, path, this));
                }
            }
        }

        @Override
        public synchronized void run() {
            final Runnable task = taskQueue.poll();
            if (task != null) {
                task.run();
            }
        }
    }

    private final DelayQueue<DeleteDelay> delayedDeletes = new DelayQueue<>();
    private final ConcurrentMap<Path, PathQueue> paths = new ConcurrentHashMap<>();
    private final Thread thread = new Thread(this);
    private volatile long reCreateTimeout;

    public void setReCreateTimeout(final long pReCreateTimeout) {
        reCreateTimeout = pReCreateTimeout;
    }

    public void start() {
        thread.setDaemon(true);
        thread.start();
    }

    public void stop() {
        thread.interrupt();
    }

    public void enque(final Path pDirectory, final WatchEvent<?> pEvent, final PathProcessingHandler pHandler) {
        final Path absolutePath = pDirectory.resolve((Path) pEvent.context());
        final PathQueue queue = paths.computeIfAbsent(absolutePath, p -> new PathQueue(absolutePath, pHandler));
        if (pEvent.count() == 1) {
            queue.addEvent(pEvent.kind(), true);
        }
    }

    @Override
    public void run() {
        try {
            while (!currentThread().isInterrupted()) {
                delayedDeletes.take().addDeleteEvent();
            }
        } catch (final InterruptedException e) {
            currentThread().interrupt();
        }
    }
}
