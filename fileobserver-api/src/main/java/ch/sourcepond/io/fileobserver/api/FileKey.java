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
package ch.sourcepond.io.fileobserver.api;

import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedList;

import static java.util.Objects.requireNonNull;

/**
 * A file key combines a watched root directory (see {@link #getDirectoryKey()}) and relative path within
 * that directory (see {@link #getRelativePath()}) to a unique identifier. Use a file key to associate a resource
 * to a specific file.
 * <p>
 * The reason why not only the relative path is used as unique file key is simple: the fileobserver implementation
 * must be able to watch more than one directory at once. This means that the same relative path possibly exists in
 * more than one watched root directory.
 * <p>
 * Another question may rise why an {@link Object} identifies a watched directory instead of an (absolute) {@link Path}.
 * The answer is that it must be possible to relocate a watched root directory during runtime. If a path would be used
 * to identify a watched root directory, it would not be possible for a {@link FileObserver} to determine which
 * resource is associated with the relocated directory.
 *
 * @param <T> Type of the directory key, see {@link #getDirectoryKey()}
 */
public interface FileKey<T> {

    /**
     * The key which represents a watched root directory.
     *
     * @return Directory-key, never {@code null}
     */
    T getDirectoryKey();

    /**
     * Returns the relative path (relative to the watched directory) of the file on which this key points to.
     * The returned value always remains the same. This is even the case when the watched directory
     * had been relocated and the file exists in the new directory.
     *
     * @return Relative path, never {@code null}
     */
    Path getRelativePath();

    /**
     * Returns the name of the farthest path element denoted by {@link #getRelativePath()}. The default
     * implementation is a shorthand for {@code getRelativePath().getFileName().toString()}.
     *
     * @return Name of the last element of {@link #getRelativePath()}, never {@code null}
     */
    default String getFileName() {
        return getRelativePath().getFileName().toString();
    }

    /**
     * Checks whether this key is a parent-key of the key specified. A key is a parent-key when:
     * <ul>
     * <li>The directory-key of {@code pOther} is equal to the directory-key of
     * this key (see {@link #getDirectoryKey()})</li>
     * <li>The relative path of {@code pOther} starts
     * with the relative path of this key (see {@link #getRelativePath()})</li></ul>
     *
     * @param pOther Other key to check whether it is a sub-key of this, must not be {@code null}
     * @return {@code true} if this key is a parent-key of the key specified, {@code false} otherwise.
     */
    default boolean isParentKeyOf(final FileKey<? extends T> pOther) {
        requireNonNull(pOther, "Other key is null");
        return getDirectoryKey().equals(pOther.getDirectoryKey()) &&
                pOther.getRelativePath().startsWith(getRelativePath());
    }

    /**
     * Checks whether this key is a sub-key of the key specified. A key is a sub-key when:
     * <ul>
     * <li>The directory-key of {@code pOther} is equal to the directory-key of
     * this key (see {@link #getDirectoryKey()})</li>
     * <li>The relative path of this key starts
     * with the relative path of {@code pOther} (see {@link #getRelativePath()})</li></ul>
     *
     * @param pOther Other key to check whether it is a parent-key of this, must not be {@code null}
     * @return {@code true} if this key is a sub-key of the key specified, {@code false} otherwise.
     */
    default boolean isSubKeyOf(final FileKey<T> pOther) {
        requireNonNull(pOther, "Other key is null");
        return getDirectoryKey().equals(pOther.getDirectoryKey()) &&
                getRelativePath().startsWith(pOther.getRelativePath());
    }

    /**
     * Creates a new collection containing all keys which are sub-keys of this key and
     * contained in the collection specified and (see {@link #isSubKeyOf(FileKey)}).
     *
     * @param pKeys Collection of potential sub-keys, must not be {@code null}
     * @return Collection of found sub-keys, possibly empty, never {@code null}
     */
    default Collection<FileKey<T>> findSubKeys(Collection<FileKey<T>> pKeys) {
        final Collection<FileKey<T>> subKeys = new LinkedList<>();
        pKeys.forEach(k -> {
            if (k.isSubKeyOf(this)) {
                subKeys.add(k);
            }
        });
        return subKeys;
    }

    /**
     * Removes all keys which are sub-keys of this key from the collection specified
     * (see {@link #isSubKeyOf(FileKey)}). If no sub-key could be found, the collection
     * specified will not be modified.
     *
     * @param pKeys Collection of potential sub-keys, must not be {@code null}
     */
    default void removeSubKeys(Collection<FileKey<T>> pKeys) {
        pKeys.removeIf(k -> k.isSubKeyOf(this));
    }
}
