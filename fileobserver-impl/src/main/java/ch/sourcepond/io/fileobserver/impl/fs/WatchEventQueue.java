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

import java.nio.file.WatchEvent;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Consumer;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

/**
 *
 */
class WatchEventQueue {
    private final Deque<WatchEvent.Kind<?>> queue = new LinkedList<>();

    private boolean isReCreate(final WatchEvent.Kind<?> pKind) {
        return ENTRY_CREATE == pKind && queue.peekLast() == ENTRY_DELETE;
    }

    private boolean isRedundant(final WatchEvent.Kind<?> pKind) {
        return ENTRY_MODIFY == pKind && ENTRY_MODIFY == queue.peekLast();
    }

    synchronized void push(final WatchEvent.Kind<?> pKind) {
        if (isReCreate(pKind)) {
            queue.removeLast();
            queue.addLast(ENTRY_MODIFY);
        } else if (!isRedundant(pKind)) {
            queue.addLast(pKind);
        }
    }

    synchronized void processQueue(final Consumer<? super WatchEvent.Kind<?>> pConsumer) {
        for (final Iterator<WatchEvent.Kind<?>> it = queue.iterator(); it.hasNext(); ) {
            pConsumer.accept(it.next());
            it.remove();
        }
    }
}
