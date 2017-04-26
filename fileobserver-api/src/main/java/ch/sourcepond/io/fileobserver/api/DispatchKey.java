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
 * A dispatch-key combines a watched directory (see {@link #getDirectoryKey()}) and the relative path of a file
 * or directory within that directory (see {@link #getRelativePath()}) to a unique key. It identifies a specific
 * file even when the underlying watched directory has been relocated. A dispatch-key should always be used for
 * any caching. Implementations of this interface ought be immutable and must override {@link Object#hashCode()} and
 * {@link Object#equals(Object)}.
 */
public interface DispatchKey {

    /**
     * <p>Returns the unique key which represents a watched directory.</p>
     * <p>The reason why using the relative path as unique identifier is not sufficient is because more than one
     * directory at the same time can be watched. This means that the same relative path possibly exists in multiple
     * observed directories.</p>
     * <p>The next question may be why instead of an absolute {@link Path} an {@link Object} identifies a watched
     * directory. The answer is that it must be possible to relocate a watched directory during runtime.
     * If an absolute path would be used to identify it, it would be impossible to a {@link PathChangeListener} to determine
     * which dispatch-keys were associated with the relocated directory. This is because they would still
     * reference the previous path. For this reason, an independent, immutable directory-key object is used to identify
     * a watched directory. The directory-key remains the same as long this object lives.</p>
     *
     * @return Directory-key, never {@code null}
     */
    Object getDirectoryKey();

    /**
     * Returns the relative path (relative to the watched directory) of the file on which this key points to.
     * The returned value always remains the same.
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
    default boolean isParentKeyOf(final DispatchKey pOther) {
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
    default boolean isSubKeyOf(final DispatchKey pOther) {
        requireNonNull(pOther, "Other key is null");
        return getDirectoryKey().equals(pOther.getDirectoryKey()) &&
                getRelativePath().startsWith(pOther.getRelativePath());
    }

    /**
     * Creates a new collection containing all keys which are sub-keys of this key and
     * contained in the collection specified and (see {@link #isSubKeyOf(DispatchKey)}).
     *
     * @param pKeys Collection of potential sub-keys, must not be {@code null}
     * @return Collection of found sub-keys, possibly empty, never {@code null}
     */
    default Collection<DispatchKey> findSubKeys(Collection<DispatchKey> pKeys) {
        final Collection<DispatchKey> subKeys = new LinkedList<>();
        pKeys.forEach(k -> {
            if (k.isSubKeyOf(this)) {
                subKeys.add(k);
            }
        });
        return subKeys;
    }

    /**
     * Removes all keys which are sub-keys of this key from the collection specified
     * (see {@link #isSubKeyOf(DispatchKey)}). If no sub-key could be found, the collection
     * specified will not be modified.
     *
     * @param pKeys Collection of potential sub-keys, must not be {@code null}
     */
    default void removeSubKeys(Collection<DispatchKey> pKeys) {
        pKeys.removeIf(k -> k.isSubKeyOf(this));
    }
}
