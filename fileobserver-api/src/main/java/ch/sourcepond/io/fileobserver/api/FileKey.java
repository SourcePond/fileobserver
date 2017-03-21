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

/**
 * A file key combines a watched root directory (see {@link #directoryKey()}) and relative path within
 * that directory (see {@link #relativePath()}) to a unique identifier. Use a file key to associate a resource
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
 */
public interface FileKey {

    /**
     * The key which represents a watched root directory.
     *
     * @return Directory-key, never {@code null}
     */
    Object directoryKey();

    /**
     * Returns the relative path (relative to the watched directory) of the file on which this key points to.
     * The returned value always remains the same. This is even the case when the watched directory
     * had been relocated and the file exists in the new directory.
     *
     * @return Relative path, never {@code null}
     */
    Path relativePath();

    /**
     * Checks whether this key is sub-key of the key specified. A key is a sub-key when:
     * <ul>
     * <li>The directory-key of {@code pOther} is equal to the directory-key of
     * this key (see {@link #directoryKey()})</li>
     * <li>The relative path of {@code pOther} starts
     * with the relative path of this key (see {@link #relativePath()})</li></ul>
     *
     * @param pOther Other key to check whether it is a parent-key of this, must not be {@code null}
     * @return {@code true} if this key is a sub-key of the key specified, {@code false} otherwise.
     */
    boolean isSubKeyOf(FileKey pOther);

    /**
     * Creates a new collection containing all keys which are sub-keys of this key and
     * contained in the collection specified and (see {@link #isSubKeyOf(FileKey)}).
     *
     * @param pKeys Collection of potential sub-keys, must not be {@code null}
     * @return Collection of found sub-keys, possibly empty, never {@code null}
     */
    Collection<FileKey> findSubKeys(Collection<FileKey> pKeys);

    /**
     * Removes all keys which are sub-keys of this key from the collection specified
     * (see {@link #isSubKeyOf(FileKey)}). If no sub-key could be found, the collection
     * specified will not be modified.
     *
     * @param pKeys Collection of potential sub-keys, must not be {@code null}
     */
    void removeSubKeys(Collection<FileKey> pKeys);
}
