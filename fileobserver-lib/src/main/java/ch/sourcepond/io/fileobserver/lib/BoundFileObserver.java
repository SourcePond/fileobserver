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
public abstract class BoundFileObserver<K, T> implements FileObserver {
    private final ConcurrentMap<Path, T> resources = new ConcurrentHashMap<>();

    protected abstract K getDirectoryKey();

    protected abstract T load(Path pFile) throws IOException;

    protected T getResource(final Path pRelativePath) {
        return resources.get(pRelativePath);
    }

    @Override
    public void modified(final FileKey pKey, final Path pFile) throws IOException {
        if (getDirectoryKey().equals(pKey.key())) {
            resources.put(pKey.relativePath(), load(pFile));
        }
    }

    @Override
    public void discard(final FileKey pKey) {
        if (getDirectoryKey().equals(pKey)) {
            final Path relativePath = pKey.relativePath();
            resources.keySet().removeIf(k -> k.startsWith(relativePath));
        }
    }
}
