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
package ch.sourcepond.io.fileobserver.impl.directory;

import ch.sourcepond.io.fileobserver.api.FileKey;

import java.nio.file.Path;
import java.nio.file.WatchKey;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Represents a root-directory i.e. a directory which has been registered to watched.
 */
public class RootDirectory extends Directory {
    private final Collection<Object> directoryKeys;
    private final DirectoryFactory factory;

    RootDirectory(final DirectoryFactory pFactory, final WatchKey pWatchKey) {
        super(pWatchKey);
        factory = pFactory;
        directoryKeys = new CopyOnWriteArraySet<>();
    }

    /**
     * This constructor is used by {@link SubDirectory#toRootDirectory()}.
     *
     * @param pFactory
     * @param pWatchKey
     * @param pDirectoryKeysOrNull
     */
    RootDirectory(final DirectoryFactory pFactory, final WatchKey pWatchKey, final Collection<Object> pDirectoryKeysOrNull) {
        super(pWatchKey);
        factory = pFactory;
        directoryKeys = pDirectoryKeysOrNull == null ? new CopyOnWriteArraySet<>() : pDirectoryKeysOrNull;
    }

    @Override
    DirectoryFactory getFactory() {
        return factory;
    }

    /**
     * <p>Adds the directory-key specified to this directory instance. When a change is detected, a
     * {@link FileKey} will be generated for every directory-key/relative-path combination.
     * This {@link FileKey} instance will then be delivered (along with the readable file path)
     * to the {@link ch.sourcepond.io.fileobserver.api.FileObserver} objects which should be informed.</p>
     *
     * <p>Note: The key object should be <em>immutable</em>, {@link String} or an {@link Enum}
     * objects are good condidates for being directory-keys.</p>
     *
     * @param pDirectoryKey Directory key, must not be {@code null}
     */
    @Override
    public void addDirectoryKey(final Object pDirectoryKey) {
        directoryKeys.add(pDirectoryKey);
    }

    /**
     * Removes the directory-key specfied from this directory instance.
     *
     * @param pDirectoryKey Directory-key to be removed, must be not {@code null}
     * @return {@code true} if this directory does not contain directory-keys anymore, {@code false} otherwise
     */
    public boolean removeDirectoryKey(final Object pDirectoryKey) {
        final Collection<Object> keys = directoryKeys;
        keys.remove(pDirectoryKey);
        return keys.isEmpty();
    }

    @Override
    Collection<Object> getDirectoryKeys() {
        return directoryKeys;
    }

    /*
     * Relativizes the path specified against the path of this directory.
     */
    @Override
    Path relativizeAgainstRoot(final Object pDirectoryKey, final Path pPath) {
        // Because we are on the last root directory possible we can ignore the
        // directory key here.
        return getPath().relativize(pPath);
    }

    @Override
    public boolean isRoot() {
        return true;
    }

    @Override
    public boolean hasKeys() {
        // A root directory has always keys
        return true;
    }

    @Override
    public Directory rebase(final Directory pBaseDirectory) {
        return new SubDirectory(pBaseDirectory, getWatchKey(), directoryKeys);
    }

    @Override
    public Directory toRootDirectory() {
        return this;
    }
}
