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

import ch.sourcepond.io.checksum.api.Resource;
import ch.sourcepond.io.fileobserver.api.DispatchKey;
import ch.sourcepond.io.fileobserver.api.PathChangeListener;
import ch.sourcepond.io.fileobserver.impl.observer.EventDispatcher;
import ch.sourcepond.io.fileobserver.spi.WatchedDirectory;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static ch.sourcepond.io.checksum.api.Algorithm.SHA256;
import static java.nio.file.Files.newDirectoryStream;
import static java.util.Collections.emptyList;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * A directory has to fulfill two purposes. If firstly holds the {@link WatchKey} of watched directory. Secondly, it
 * stores the checksums of changed files.
 */
public abstract class Directory {
    private static final Logger LOG = getLogger(SubDirectory.class);
    private final ConcurrentMap<Path, Resource> resources = new ConcurrentHashMap<>();
    private final WatchKey watchKey;

    Directory(final WatchKey pWatchKey) {
        assert pWatchKey != null : "pWatchKey";
        watchKey = pWatchKey;
    }

    /**
     * Iterates over all files contained by this directory and informs for each entry
     * the currently focused observer. Only direct children will be considered,
     * sub-directories and non-regular files will be ignored.
     */
    private void streamDirectoryAndForceInform(final EventDispatcher pDispatcher) {
        try (final DirectoryStream<Path> stream = newDirectoryStream(getPath(), Files::isRegularFile)) {
            stream.forEach(p ->
                    createKeys(p).forEach(k ->
                            pDispatcher.modified(k, p, emptyList())));
        } catch (final IOException e) {
            LOG.warn("Exception occurred while trying to inform single observers!", e);
        }
    }

    /**
     * <p><em>INTERNAL API, only ot be used in class hierarchy</em></p>
     * <p>
     * Returns the registered watched-directories (see {@link #addWatchedDirectory(WatchedDirectory)}). Any change
     * on the returned collection could possible change the internal state.
     *
     * @return (Possibly empty) collection of watched-directories, never {@code null}
     */
    abstract Collection<WatchedDirectory> getWatchedDirectories();

    abstract DirectoryFactory getFactory();

    /**
     * <p><em>INTERNAL API, only ot be used in class hierarchy</em></p>
     * <p>
     * Relatives the first root-directory against the path specified. To determine which is the first
     * root-directory, the key returned by {@link WatchedDirectory#getKey()} of the watched-directoriy
     * specified will be matched against every directory in the tree. If a directory directly
     * contains the key, it will be used for relativization.
     *
     * @param pPath             Path to be relativized, must not be {@code null}
     * @param pWatchedDirectory Associated watched-directory of the desired root directory, must not be {@code null}
     * @return Relative path between root and the path specified, never {@code null}.
     */
    abstract Path relativizeAgainstRoot(WatchedDirectory pWatchedDirectory, Path pPath);

    /**
     * <p><em>INTERNAL API, only ot be used in class hierarchy</em></p>
     * <p>
     * Creates a new collection of {@link DispatchKey} objects. Therefore, the directory-key of every watched-directory
     * returned by {@link #getWatchedDirectories()} will be combined with the relative path of the
     * file specified. If a watched-directory does blacklist the file specified, no key for that directory will be
     * generated. The relative path is the relativization between the root-directory and
     * the file specified (see {@link #relativizeAgainstRoot(WatchedDirectory, Path)}).
     *
     * @param pFile File to relativize against {@link #getPath()}, must not be {@code null}
     * @return New collection of {@link DispatchKey} objects, never {@code null}
     */
    private Collection<DispatchKey> createKeys(final Path pFile) {
        final Collection<WatchedDirectory> watchedDirectories = getWatchedDirectories();
        final List<DispatchKey> keys = new ArrayList<>(watchedDirectories.size());
        for (final WatchedDirectory watchedDirectory : watchedDirectories) {
            final Path relativePath = relativizeAgainstRoot(watchedDirectory, pFile);

            if (!watchedDirectory.isBlacklisted(relativePath)) {
                keys.add(getFactory().newKey(watchedDirectory.getKey(), relativePath));
            } else {
                LOG.info("{} is blacklisted by {}", relativePath, watchedDirectory);
            }
        }

        return keys;
    }

    /**
     * <p><em>INTERNAL API, only ot be used in class hierarchy</em></p>
     * <p>
     * Returns the {@link WatchKey} which is associated with this directory.
     * The watch-key remains accessible even afeter {@link #cancelKey()} has been called.
     *
     * @return Watch-key, never {@code null}
     */
    WatchKey getWatchKey() {
        return watchKey;
    }

    public abstract boolean isRoot();

    public abstract boolean hasKeys();

    /**
     * <p>Adds the watched-directory specified to this directory instance. When a change is detected, a
     * {@link DispatchKey} will be generated for every {@link WatchedDirectory#getKey()}/relative-path combination.
     * This {@link DispatchKey} instance will then be delivered (along with the readable file path)
     * to the {@link PathChangeListener} objects which should be informed.</p>
     * <p>
     * <p>Note: The object returned by {@link WatchedDirectory#getKey()} should be <em>immutable</em>,
     * {@link String} or an {@link Enum} objects are good condidates for being directory-keys.</p>
     *
     * @param pDirectoryKey Directory key, must not be {@code null}
     */
    public abstract void addWatchedDirectory(WatchedDirectory pDirectoryKey);

    abstract long getTimeout();

    /**
     * <p><em>INTERNAL API, only ot be used in class hierarchy</em></p>
     * <p>
     * Removes the directory-key specified from this directory instance. If no such
     * key is registered nothing happens.
     *
     * @param pDirectoryKey Directory-key to be removed, must be not {@code null}
     * @return {@code true} if the directory-key specified was removed, {@code false} otherwise.
     */
    abstract boolean remove(WatchedDirectory pDirectoryKey);

    /**
     * Removes the watched-directory specified from this directory instance and informs
     * the observers specified through their {@link PathChangeListener#discard(DispatchKey)}. If no such
     * watched-directory is registered nothing happens.
     *
     * @param pWatchedDirectory Directory-key to be removed, must be not {@code null}
     */
    public void removeWatchedDirectory(final EventDispatcher pDispatcher, final WatchedDirectory pWatchedDirectory) {
        // It is important to evaluate to relative path before the
        // has been removed otherwise the resulting key has not the
        // expected value!
        final Path relativePath = relativizeAgainstRoot(pWatchedDirectory, getPath());

        // Now, the key can be safely removed
        if (remove(pWatchedDirectory) && pDispatcher.hasObservers()) {
            final DispatchKey key = getFactory().newKey(pWatchedDirectory.getKey(), relativePath);
            pDispatcher.discard(key);
        }
    }

    /**
     * Cancels the {@link WatchKey} held by this directory object (see {@link WatchKey#cancel()}).
     * After this, checksum resources are cleared and no more events for this directory can be retrieved.
     */
    public void cancelKey() {
        try {
            getWatchKey().cancel();
        } finally {
            resources.clear();
        }
    }

    /**
     * Iterates over the files contained by this directory and creates tasks which will be executed
     * sometime in the future. Such a task will inform the observer specified through its
     * {@link PathChangeListener#modified(ch.sourcepond.io.fileobserver.api.DispatchEvent)} method. Note: only direct children will be
     * considered, sub-directories and non-regular files will be ignored.
     */
    public void forceInform(final EventDispatcher pDispatcher) {
        getFactory().executeDirectoryWalkerTask(() -> streamDirectoryAndForceInform(pDispatcher));
    }

    /**
     * Returns the path represented by this directory object.
     *
     * @return Path of this directory, never {@code null}.
     */
    public Path getPath() {
        return (Path) getWatchKey().watchable();
    }

    /**
     * Iterates over the observers specified and informs them that the file specified has
     * been discarded through their {@link PathChangeListener#discard(DispatchKey)} method. The observers
     * will be called asynchronously sometime in the future.
     *
     * @param pFile Discarded file, must be {@code null}
     */
    public void informDiscard(final EventDispatcher pDispatcher, final Path pFile) {
        // Remove the checksum resource to save memory
        resources.remove(pFile);

        if (pDispatcher.hasObservers()) {
            createKeys(pFile).forEach(pDispatcher::discard);
        }
    }

    /**
     * Checks whether this directory is the directory parent of the directory specified.
     *
     * @param pOther Other directory, must not be {@code null}
     * @return {@code true} if this object is the direct parent of the directory specified, {@code false} otherwise
     */
    public boolean isDirectParentOf(Directory pOther) {
        return getPath().equals(pOther.getPath().getParent());
    }

    public Resource getResource(final Path pFile) {
        return resources.computeIfAbsent(pFile, f -> getFactory().newResource(SHA256, pFile));
    }

    /**
     * Triggers the {@link PathChangeListener#modified(ch.sourcepond.io.fileobserver.api.DispatchEvent)} on all observers specified if the
     * file represented by the path specified has been changed i.e. has a new checksum. If no checksum change
     * has been detected, nothing happens.
     *
     * @param pFile File which potentially has changed, must not be {@code null}
     */
    public void informIfChanged(final EventDispatcher pDispatcher, final Directory pNewRootOrNull, final Path pFile) {
        if (pDispatcher.hasObservers()) {
            try {
                getResource(pFile).update(getTimeout(),
                        update -> {
                            if (update.hasChanged()) {
                                // If the modification is requested because a new root-directory has been registered, we
                                // need to inform the observers about supplement keys.
                                final Collection<DispatchKey> supplementKeys = pNewRootOrNull == null ?
                                        emptyList() : pNewRootOrNull.createKeys(pFile);

                                createKeys(pFile).forEach(k -> pDispatcher.modified(k, pFile, supplementKeys));
                            }
                        });
            } catch (final IOException e) {
                LOG.warn(e.getMessage(), e);
            }
        }
    }

    /**
     * Triggers the {@link PathChangeListener#modified(ch.sourcepond.io.fileobserver.api.DispatchEvent)} on all observers specified if the
     * file represented by the path specified has been changed i.e. has a new checksum. If no checksum change
     * has been detected, nothing happens.
     *
     * @param pFile File which potentially has changed, must not be {@code null}
     */
    public void informIfChanged(final EventDispatcher pDispatcher, final Path pFile) {
        informIfChanged(pDispatcher, null, pFile);
    }

    public abstract Directory rebase(Directory pBaseDirectory);

    public abstract Directory toRootDirectory();
}
