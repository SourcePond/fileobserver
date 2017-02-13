package ch.sourcepond.io.fileobserver.impl.directory;

import ch.sourcepond.io.checksum.api.Algorithm;
import ch.sourcepond.io.checksum.api.Checksum;
import ch.sourcepond.io.checksum.api.Resource;
import ch.sourcepond.io.fileobserver.api.FileKey;
import ch.sourcepond.io.fileobserver.api.FileObserver;

import java.nio.file.Path;
import java.nio.file.WatchKey;
import java.util.Collection;

/**
 * Created by rolandhauser on 10.02.17.
 */
public class FsRootDirectory extends FsBaseDirectory {
    private final FsDirectoryFactory factory;
    private final Enum<?> watchedDirectoryKey;
    private volatile WatchKey watchKey;

    FsRootDirectory(final FsDirectoryFactory pFactory, final Enum<?> pWatchedDirectoryKey) {
        factory = pFactory;
        watchedDirectoryKey = pWatchedDirectoryKey;
    }

    public void setWatchKey(final WatchKey pWatchKey) {
        watchKey = pWatchKey;
    }

    @Override
    WatchKey getWatchKey() {
        return watchKey;
    }

    @Override
    Enum<?> getWatchedDirectoryKey() {
        return watchedDirectoryKey;
    }

    @Override
    Resource newResource(final Algorithm pAlgorithm, final Path pFile) {
        return factory.newResource(pAlgorithm, pFile);
    }

    @Override
    public FileKey newKey(final Path pFile) {
        return factory.newKey(watchedDirectoryKey, getPath().relativize(pFile));
    }

    @Override
    void informObservers(final Checksum pPrevious, final Checksum pCurrent, final Collection<FileObserver> pObservers, final Path pFile) {
        if (!pPrevious.equals(pCurrent)) {
            final FileKey key = newKey(pFile);
            pObservers.forEach(o -> factory.execute(() -> o.modified(key, pFile)));
        }
    }

    @Override
    public void forceInformObservers(final Collection<FileObserver> pObservers, final Path pFile) {
        final FileKey key = newKey(pFile);
        pObservers.forEach(o -> factory.execute(() -> o.modified(key, pFile)));
    }
}
