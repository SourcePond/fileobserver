package ch.sourcepond.io.fileobserver.impl.directory;

import ch.sourcepond.io.checksum.api.Algorithm;
import ch.sourcepond.io.checksum.api.Checksum;
import ch.sourcepond.io.checksum.api.Resource;
import ch.sourcepond.io.fileobserver.api.FileKey;
import ch.sourcepond.io.fileobserver.api.FileObserver;

import java.nio.file.Path;
import java.nio.file.WatchKey;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import static java.util.stream.Collectors.toList;

/**
 * Created by rolandhauser on 10.02.17.
 */
public class FsRootDirectory extends FsBaseDirectory {
    private final Set<Object> directoryKeys = new CopyOnWriteArraySet<>();
    private final FsDirectoryFactory factory;
    private volatile WatchKey watchKey;

    FsRootDirectory(final FsDirectoryFactory pFactory) {
        factory = pFactory;
    }

    @Override
    void addDirectoryKey(final Object pKey) {
        directoryKeys.add(pKey);
    }

    @Override
    Collection<Object> getDirectoryKeys() {
        return directoryKeys;
    }

    public void setWatchKey(final WatchKey pWatchKey) {
        watchKey = pWatchKey;
    }

    @Override
    WatchKey getWatchKey() {
        return watchKey;
    }

    @Override
    Resource newResource(final Algorithm pAlgorithm, final Path pFile) {
        return factory.newResource(pAlgorithm, pFile);
    }

    @Override
    public Collection<FileKey> createKeys(final Path pFile) {
        return directoryKeys.
                stream().map(
                k -> factory.newKey(k, getPath().relativize(pFile))).
                collect(toList());
    }

    @Override
    void informObservers(final Checksum pPrevious, final Checksum pCurrent, final Collection<FileObserver> pObservers, final Path pFile) {
        if (!pPrevious.equals(pCurrent)) {
            forceInformObservers(pObservers, pFile);
        }
    }

    @Override
    public void forceInformObservers(final Collection<FileObserver> pObservers, final Path pFile) {
        for (final FileKey key : createKeys(pFile)) {
            pObservers.forEach(o -> factory.execute(() -> o.modified(key, pFile)));
        }
    }
}
