package ch.sourcepond.io.fileobserver.impl;

import org.slf4j.Logger;

import java.nio.file.Path;
import java.nio.file.WatchKey;

import static org.slf4j.LoggerFactory.getLogger;

/**
 *
 */
class FsDirectory  {
    private static final Logger LOG = getLogger(FsDirectory.class);
    private final FsDirectory parent;
    private final WatchKey key;

    FsDirectory(final FsDirectory pParent, final WatchKey pKey) {
        parent = pParent;
        key = pKey;
    }

    Path getPath() {
        return (Path)key.watchable();
    }

    private Path findRoot() {
        Path rootDir = getPath();
        FsDirectory dir = parent;
        while (null != dir) {
            rootDir = dir.getPath();
            dir = dir.parent;
        }
        return rootDir;
    }

    void cancelKey() {
        key.cancel();
    }

    String relativize(final Path pFile) {
        return findRoot().relativize(pFile).toString();
    }
}
