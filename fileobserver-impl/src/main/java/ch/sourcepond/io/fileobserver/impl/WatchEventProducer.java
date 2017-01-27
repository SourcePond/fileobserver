package ch.sourcepond.io.fileobserver.impl;

import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

import static java.lang.Thread.currentThread;
import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.Files.isDirectory;
import static java.nio.file.Files.walkFileTree;
import static java.nio.file.StandardWatchEventKinds.*;
import static org.slf4j.LoggerFactory.getLogger;

/**
 *
 */
public class WatchEventProducer implements Runnable {
    private static final Logger LOG = getLogger(WatchEventProducer.class);
    private final Map<WatchKey, Path> directories = new HashMap<>();
    private final WatchService watchService;
    private final ResourceEventProducer resourceEventProducer;

    public WatchEventProducer(final WatchService pWatchService, final ResourceEventProducer pResourceEventProducer) {
        watchService = pWatchService;
        resourceEventProducer = pResourceEventProducer;
    }

    private Iterable<? extends WatchEvent<?>> pollEvents(final WatchKey watchKey) {
        return watchKey.pollEvents();
    }

    private void registerDirectories(final Path pDirectory) {
        try {
            walkFileTree(pDirectory, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
                    directories.put(dir.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY), dir);
                    return CONTINUE;
                }
            });
        } catch (final IOException e) {
            LOG.warn(e.getMessage(), e);
        }
    }

    @Override
    public void run() {
        try {
            while (!currentThread().isInterrupted()) {
                final WatchKey watchKey = watchService.take();
                final Path directory = directories.get(watchKey);
                if (null == directory) {
                    LOG.warn("Watch-key is unknown!");
                    continue;
                }
                try {
                    for (final WatchEvent<?> event : pollEvents(watchKey)) {
                        final WatchEvent.Kind<?> kind = event.kind();
                        // This key is registered only
                        // for ENTRY_CREATE events,
                        // but an OVERFLOW event can
                        // occur regardless if events
                        // are lost or discarded.
                        if (OVERFLOW == kind) {
                            continue;
                        }

                        // The filename is the
                        // context of the event.
                        final Path child = directory.resolve((Path)event.context());
                        if (ENTRY_CREATE == kind && isDirectory(child)) {
                            registerDirectories(child);
                        } else if (ENTRY_CREATE == kind || ENTRY_MODIFY == kind) {
                            resourceEventProducer.fileModify(child);
                        } else if (ENTRY_DELETE == kind) {
                            resourceEventProducer.fileDelete(child);
                        }
                    }
                } finally {
                    // Reset the key -- this step is critical if you want to
                    // receive further watch events.  If the key is no longer valid,
                    // the directory is inaccessible so exit the loop.
                    if (!watchKey.reset()) {
                        break;
                    }
                }

            }
        } catch (final ClosedWatchServiceException | InterruptedException e) {
            currentThread().interrupt();
            LOG.warn(e.getMessage(), e);
        }
    }
}
