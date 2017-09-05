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

import ch.sourcepond.io.fileobserver.impl.Config;
import ch.sourcepond.io.fileobserver.impl.listener.ListenerManager;
import org.slf4j.Logger;

import java.io.Closeable;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.util.concurrent.DelayQueue;
import java.util.function.Consumer;

import static java.lang.String.format;
import static java.lang.Thread.currentThread;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;
import static org.slf4j.LoggerFactory.getLogger;

/**
 *
 */
public class DelayedPathChangeDispatcher implements Closeable {

    @FunctionalInterface
    private interface Supplier<T> {

        T get() throws InterruptedException;
    }

    private static final Logger LOG = getLogger(DelayedPathChangeDispatcher.class);
    private final WatchServiceWrapper wrapper;
    private final PathChangeHandler pathChangeHandler;
    private final ListenerManager manager;
    private final FileSystemEventFactory eventFactory;
    private final Thread receiverThread;
    private final Thread dispatcherThread;
    private final DelayQueue<FileSystemEvent> events = new DelayQueue<>();

    DelayedPathChangeDispatcher(final WatchServiceWrapper pWrapper,
                                final PathChangeHandler pPathChangeHandler,
                                final ListenerManager pManager,
                                final FileSystemEventFactory pEventFactory) {
        wrapper = pWrapper;
        pathChangeHandler = pPathChangeHandler;
        manager = pManager;
        eventFactory = pEventFactory;
        receiverThread = new Thread(this::receive, format("fs-event receiver %s", wrapper));
        dispatcherThread = new Thread(this::dispatch, format("fs-event dispatcher %s", wrapper));
    }

    private void dispatchEvent(final FileSystemEvent pEvent) {
        final WatchEvent.Kind<?> kind = pEvent.getKind();
        final Path child = pEvent.getPath();
        LOG.debug("Received event of kind {} for path {}", kind, pEvent.getPath());
        try {
            if (ENTRY_CREATE == kind) {
                pathChangeHandler.pathModified(manager.getDefaultDispatcher(), child, true);
            } else if (ENTRY_MODIFY == kind) {
                pathChangeHandler.pathModified(manager.getDefaultDispatcher(), child, false);
            } else if (ENTRY_DELETE == kind) {
                pathChangeHandler.pathDiscarded(manager.getDefaultDispatcher(), child);
            }
        } catch (final RuntimeException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    private void delayReceived(final WatchKey pWatchKey) {
        final Path directory = (Path) pWatchKey.watchable();
        for (final WatchEvent<?> event : pWatchKey.pollEvents()) {
            final WatchEvent.Kind<?> kind = event.kind();

            if (LOG.isDebugEnabled()) {
                LOG.debug(format("Changed detected [%s]: %s, context: %s", kind, directory, event.context()));
            }

            // An OVERFLOW event can
            // occur regardless if events
            // are lost or discarded.
            if (OVERFLOW == kind || event.count() > 1) {
                continue;
            }

            final FileSystemEvent fsEvent = eventFactory.newEvent(kind,
                    directory.resolve((Path) event.context()));

            if (!events.contains(fsEvent)) {
                events.add(fsEvent);
            }
        }

        // The case when the WatchKey has been cancelled is
        // already handled at a different place.
        pWatchKey.reset();
    }

    private void shutdown(final Thread pThread) {
        if (!pThread.isInterrupted()) {
            pThread.interrupt();
        }
    }

    private void startThread(final Thread pThread) {
        pThread.setDaemon(true);
        pThread.start();
    }

    public void start() {
        startThread(dispatcherThread);
        startThread(receiverThread);
    }

    /**
     * <p>Stops the scanner threads which observe the watched directories for changes.</p>
     * <p>
     * <p>This must be named "stop" in order to be called from Felix DM (see
     * <a href="http://felix.apache.org/documentation/subprojects/apache-felix-dependency-manager/reference/components.html">Dependency Manager - Components</a>)</p>
     */
    @Override
    public void close() {
        shutdown(receiverThread);
        shutdown(dispatcherThread);
        wrapper.close();
    }

    private void close(final Exception e) {
        close();
        LOG.debug(e.getMessage(), e);
    }

    private <T> void run(final Supplier<T> pSupplier, final Consumer<T> pConsumer) {
        while (!currentThread().isInterrupted()) {
            try {
                pConsumer.accept(pSupplier.get());
            } catch (final ClosedWatchServiceException | InterruptedException e) {
                close(e);
            }
        }
        LOG.info("Stopped {}", currentThread().getName());
    }

    private void receive() {
        run(wrapper::take, this::delayReceived);
    }

    private void dispatch() {
        run(events::take, this::dispatchEvent);
    }
}
