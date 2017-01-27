package ch.sourcepond.io.fileobserver.impl;

import ch.sourcepond.io.checksum.api.CalculationObserver;
import ch.sourcepond.io.checksum.api.Checksum;
import ch.sourcepond.io.checksum.api.Resource;
import ch.sourcepond.io.checksum.api.ResourcesFactory;
import ch.sourcepond.io.fileobserver.api.PathObserver;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.concurrent.*;
import java.util.function.Consumer;

import static ch.sourcepond.io.checksum.api.Algorithm.SHA256;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * This class manages {@link PathObserver} instances and delivers file modifications/deletions to any interested
 * observer.
 */
class ResourceEventProducer {
    private final ConcurrentMap<Path, Resource> resources = new ConcurrentHashMap<>();
    private final ConcurrentMap<Path, Object> calculations = new ConcurrentHashMap<>();
    private final Collection<PathObserver> observers = new CopyOnWriteArraySet<>();
    private final ResourcesFactory resourceFactory;
    private final KeyRegistry keyRegistry;
    private final ExecutorService observerExecutor;
    private final ScheduledExecutorService updateScheduler;

    public ResourceEventProducer(final ResourcesFactory pResourcesRegistry,
                                 final KeyRegistry pKeyRegistry,
                                 final ExecutorService pObserverExecutor,
                                 final ScheduledExecutorService pUpdateExecutor) {
        resourceFactory = pResourcesRegistry;
        keyRegistry = pKeyRegistry;
        observerExecutor = pObserverExecutor;
        updateScheduler = pUpdateExecutor;
    }

    /**
     * Updates the checksum of the relative path specified. After the update successfully completes,
     * all interested {@link PathObserver} will be informed, see {@link #done(Checksum, Checksum, Path, Consumer)}.
     *
     * @param pRelativePath Relative path of the file (relative to the watched root directory), must not be {@code null}.
     * @param pConsumer     Function reference to {@link PathObserver#modified(Path)} or
     *                      {@link PathObserver#deleted(Path)}, never {@code null}
     */
    private void updateResource(final Path pRelativePath, final Consumer<PathObserver> pConsumer) {
        // Get resource in a concurrency-safe manner
        Resource resource = resources.get(pRelativePath);
        if (null == resource) {
            resource = resourceFactory.create(SHA256, pRelativePath);
            final Resource current = resources.putIfAbsent(pRelativePath, resource);
            if (null != current) {
                resource = current;
            }
        }

        // Update the checksum
        // TODO: replace fixed interval value with a configurable value
        resource.update(2000,
                (pPrevious, pCurrent) -> done(pPrevious, pCurrent, pRelativePath, pConsumer));
    }

    /**
     * May trigger a new checksum update for the file specified. An update is only scheduled if and only if no update
     * is currently running for the file specified. If currently an update is in progress, calling this method has
     * no effect.
     * <p>
     * The update is delayed for a short timeout to ensure that the latest
     * fs-event is handled.
     *
     * @param pRelativePath Relative path of the file (relative to the watched root directory), must not be {@code null}.
     * @param pConsumer     Type of the event which may be fired, must not be {@code null}.
     */
    private void mayTriggerUpdate(final Path pRelativePath, final Consumer<PathObserver> pConsumer) {
        // TODO: replace fixed delay value with a configurable value
        calculations.computeIfAbsent(pRelativePath,
                f -> updateScheduler.schedule(
                        () -> updateResource(pRelativePath, pConsumer), 200, MILLISECONDS));
    }

    /**
     * Adds a {@link PathObserver} to this object. The newly registered observer receive a
     * call to {@link PathObserver#modified(Path)} for every existing file in the watched directories.
     *
     * @param pObserver
     * @throws NullPointerException Thrown, if the observer specified is {@code null}.
     * @throws NoSuchFileException
     */
    void addObserver(final PathObserver pObserver) throws IOException {
        requireNonNull(pObserver, "Observer is null");
        if (observers.add(pObserver)) {
            keyRegistry.walkFiles(pObserver.getKeys(), f -> {
                observerExecutor.execute(() -> {
                            if (pObserver.accept(f)) {
                                pObserver.modified(f);
                            }
                        }
                );
            });
        }
    }

    void fileDelete(final Path pRelativePath) {
        mayTriggerUpdate(pRelativePath, l -> l.deleted(pRelativePath));
    }

    void fileModify(final Path pRelativePath) {
        mayTriggerUpdate(pRelativePath, l -> l.modified(pRelativePath));
    }

    /**
     * Delivers the relative path specified to all interested observers. If an observer accepts the path (
     * see {@link PathObserver#accept(Path)}), then an asynchronous task will be executed which
     * eventually calls {@link PathObserver#modified(Path)} or {@link PathObserver#deleted(Path)}
     * depending whether a file has been modified or deleted.
     *
     * @param pObserver     Observer to be potentially informed, never {@code null}
     * @param pRelativePath Relative path of the file (relative to the watched root directory) which has been
     *                      modified or deleted, never {@code null}.
     * @param pConsumer     Function reference to {@link PathObserver#modified(Path)} or
     *                      {@link PathObserver#deleted(Path)}, never {@code null}
     */
    private void informObserver(final PathObserver pObserver, final Path pRelativePath, final Consumer<PathObserver> pConsumer) {
        observerExecutor.execute(() -> {
            if (pObserver.accept(pRelativePath)) {
                pConsumer.accept(pObserver);
            }
        });
    }

    /**
     * This is the callback method which will be executed when {@link Resource#update(long, CalculationObserver)}
     * successfully completes.
     *
     * @param pPrevious     The checksum which was valid <em>before</em> the update
     * @param pCurrent      The checksum which is valid <em>after</em> the update.
     * @param pRelativePath Modified or deleted path.
     */

    private void done(final Checksum pPrevious, final Checksum pCurrent, final Path pRelativePath, Consumer<PathObserver> pConsumer) {
        calculations.remove(pRelativePath);
        if (!pPrevious.equals(pCurrent)) {
            observers.forEach(l -> informObserver(l, pRelativePath, pConsumer));
        }
    }
}
