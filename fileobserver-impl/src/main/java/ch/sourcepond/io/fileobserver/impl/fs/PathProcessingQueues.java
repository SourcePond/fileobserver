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
import java.nio.file.WatchEvent;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;

import static java.nio.file.StandardWatchEventKinds.*;

/**
 *
 */
public class PathProcessingQueues {
    /**
     *
     */
    private class PathQueue implements PendingEventDone {
        private final Queue<Runnable> taskQueue = new LinkedBlockingQueue<>();
        private final Path path;
        private final PathProcessingHandler handler;
        private WatchEvent.Kind<?> latestEvent;

        PathQueue(final Path pPath, final PathProcessingHandler pHandler) {
            path = pPath;
            handler = pHandler;
        }

        private boolean isReCreate(final WatchEvent.Kind<?> pKind) {
            return ENTRY_CREATE.equals(pKind) &&  ENTRY_DELETE.equals(latestEvent);
        }

        synchronized void addEvent(final WatchEvent.Kind<?> pKind) {
            final WatchEvent.Kind<?> virtualKind = isReCreate(pKind) ? ENTRY_MODIFY : pKind;
            latestEvent = virtualKind;
            paths.putIfAbsent(path, this);

            if (taskQueue.isEmpty()) {
                handler.process(pKind, path, this);
            } else {
                taskQueue.offer(() -> handler.process(pKind, path, this));
            }
        }

        @Override
        public synchronized void done() {
            final Runnable task = taskQueue.poll();
            if (task != null) {
                task.run();
            } else {
                paths.remove(path);
            }
        }
    }

    private final ConcurrentMap<Path, PathQueue> paths = new ConcurrentHashMap<>();

    public void enque(final Path pDirectory, final WatchEvent<?> pEvent, final PathProcessingHandler pHandler) {
        final Path absolutePath = pDirectory.resolve((Path) pEvent.context());
        final PathQueue queue = paths.computeIfAbsent(absolutePath, p -> new PathQueue(absolutePath, pHandler));
        if (pEvent.count() == 1) {
            queue.addEvent(pEvent.kind());
        }
    }
}
