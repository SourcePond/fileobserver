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

import ch.sourcepond.io.checksum.api.UpdateObserver;

import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.DelayQueue;

import static java.lang.Thread.currentThread;
import static java.util.concurrent.ConcurrentHashMap.newKeySet;

/**
 * <p>This class solves the <a href="http://stackoverflow.com/questions/16777869/java-7-watchservice-ignoring-multiple-occurrences-of-the-same-event">"2 events in short time"</a> issue
 * experienced on different platform. If a create event is detected, it can be registered
 * with an object of class this class through {@link #registerCreateEvent(Path)}. After that, it's not allowed to
 * process a modification event during the specified timeout (see {@link #isModificationAllowed(Path)} and {@link #setTimoutInMilliseconds(long)}).</p>
 * <p>Together with {@link ch.sourcepond.io.checksum.api.Resource#update(long, UpdateObserver)} it's guaranteed
 * that no content change is missed.</p>
 *
 */
public class PendingEventRegistry implements Runnable {

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

    void registerCreateEvent(final Path pPath) {
        if (!thread.isAlive()) {
            throw new IllegalStateException("Thread not alive");
        }
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
