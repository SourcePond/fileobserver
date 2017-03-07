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

import java.io.IOException;
import java.nio.file.Path;

/**
 * <p>Observer to receive notifications about changes on files
 * within a watched directory and its sub-directories.</p>
 *
 * <p><em>Implementations of this interface must be thread-safe.</em></p>
 */
public interface FileObserver {

    /**
     * <p>
     * Indicates, that the file specified has been modified. Modified means,
     * that the file has been created or updated. This method takes two parameters:
     * <p>
     * <h3>Relative path</h3>
     * This path is relative to the watched directory. This path <em>cannot</em> be used to read any data.
     * The relative path always remains the same for a specific file, even when the underlying
     * watched directory (and therefore the absolute file) has been updated to point to another location.
     * Because this, use the relative path for any caching of objects created out of the file data.
     * <p>
     * <h3>Readable Path</h3>
     * This is the (absolute) path which can be opened for reading. The readable path of a file can change in
     * case when the underlying watched directory (and therefore the absolute file) is updated to point to another
     * location. Because this, do <em>not</em> use the readable path for any caching, but, only for reading (or writing)
     * data.
     * <p>
     * <p>
     * Following code snipped should give an idea how caching of an object created out of the readable path
     * should be implemented:
     * <pre>
     *      final Map&lt;FileKey, Object&gt; cache = ...
     *      cache.put(pKey, readObject(pFile));
     * </pre>
     *
     * @param pKey  File-key of the modified file, never {@code null}
     * @param pFile Readable path, never {@code null}
     * @throws IOException Thrown, if the modified path could not be read.
     */
    void modified(FileKey pKey, Path pFile) throws IOException;

    /**
     * <p>Indicates, that the file or directory with the {@link FileKey} specified has been discarded for some reason
     * (file/directory has been deleted, watched directory is being unregistered etc.). In case a directory is being
     * discarded, only the file-key of that directory will be delivered. This means, that this method will <em>not</em>
     * be called for any file or sub-directory within the discarded directory.
     * <p>
     * <p>Following code snipped should give an idea how to properly remove all resources which are related to
     * the path specified from a cache:
     * <pre>
     *      final Map&lt;FileKey, Object&gt; cache = ...
     *
     *      // Remove all keys which are a child of the
     *      // file-key specified.
     *      cache.keySet().removeIf(k -&gt; p.isSubKey(pKey));
     * </pre>
     *
     * @param pKey File-key of the discarded file or directory, never {@code null}
     */
    void discard(FileKey pKey);

    /**
     * <p>Advices this observer that the supplement key specified supplements the known key specified.</p>
     *
     * <p>Explanation: bundle A registers a watched directory with path "/A/B/C". Later, bundle B registers a watched directory
     * with path "/A". Both of this directories are located in the same file-system. This means, when absolute
     * path /A/B/C/foo/bar.txt had been changed, the observers would be informed twice, one time with relative path
     * "foo/bar.txt" and one time with relative path "B/C/foo/bar.txt". This could lead to disproportional memory
     * usage and worse performance because the observers would take and action multiple times on the same content.</p>
     *
     * <p>To avoid this, an implementation class can implement this method to react properly on supplement keys. It
     * will be informed about keys which supplement already known keys <em>before</em> {@link #modified(FileKey, Path)}
     * is being called.</p>
     *
     * @param pKnownKey Key which has already been delivered to this observer, never {@code null}
     * @param pSupplementKey Key which is supplements the known key specified, never {@code null}
     */
    default void supplement(FileKey pKnownKey, FileKey pSupplementKey) {
        // Implementation of the method is optional

    }
}
