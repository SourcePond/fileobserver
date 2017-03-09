package ch.sourcepond.io.fileobserver.lib;

import ch.sourcepond.io.fileobserver.api.FileKey;
import ch.sourcepond.io.fileobserver.api.FileObserver;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by rolandhauser on 05.03.17.
 */
public abstract class BaseFileObserver<T> implements FileObserver {
    private final ConcurrentMap<Object, ConcurrentMap<Path, T>> resources = new ConcurrentHashMap<>();

    private ConcurrentMap<Path, T> getMap(final Object pDirectoryKey) {
        return resources.computeIfAbsent(pDirectoryKey, k -> new ConcurrentHashMap<Path, T>());
    }

    protected abstract T load(Path pFile) throws IOException;

    protected T getResource(final Object pDirectoryKey, final Path pRelativePath) {
        return getMap(pDirectoryKey).get(pRelativePath);
    }

    @Override
    public void modified(final FileKey pKey, final Path pFile) throws IOException {
        getMap(pKey.key()).put(pKey.relativePath(), load(pFile));
    }

    @Override
    public void discard(final FileKey pKey) {
        final ConcurrentMap<Path, T> map = resources.get(pKey.key());
        if (map != null) {
            final Path relativePath = pKey.relativePath();
            map.keySet().removeIf(k -> k.startsWith(relativePath));
        }
    }

    @Override
    public void supplement(final FileKey pKnownKey, final FileKey pSupplementKey) {
        final ConcurrentMap<Path, T> map = getMap(pSupplementKey.key());
        map.forEach((k, v) -> {
            if (k.startsWith(pKnownKey.relativePath())) {
                map.putIfAbsent(pSupplementKey.relativePath(), v);
            }
        });
    }
}
