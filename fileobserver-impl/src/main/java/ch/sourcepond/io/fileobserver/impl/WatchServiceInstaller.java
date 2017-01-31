package ch.sourcepond.io.fileobserver.impl;

import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

import static com.sun.nio.file.SensitivityWatchEventModifier.HIGH;
import static java.lang.String.format;
import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.Files.walkFileTree;
import static java.nio.file.StandardWatchEventKinds.*;
import static org.slf4j.LoggerFactory.getLogger;

/**
 *
 */
class WatchServiceInstaller {
    private static final Logger LOG = getLogger(WatchServiceInstaller.class);
    private final Map<Path, WatchKey> keys = new HashMap<>();
    private final WatchService watchService;
    private final Path directory;

    WatchServiceInstaller(final WatchService pWatchService, final Path pDirectory) {
        watchService = pWatchService;
        directory = pDirectory;
    }

    Map<Path, WatchKey> registerDirectories() throws IOException {
        walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
                keys.put(dir, dir.register(watchService, new WatchEvent.Kind[]{ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY
                }, HIGH));

                if (LOG.isDebugEnabled()) {
                    LOG.debug(format("Installed watch-service for %s", dir));
                }

                return CONTINUE;
            }
        });
        return keys;
    }
}
