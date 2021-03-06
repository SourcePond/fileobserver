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
import java.nio.file.FileSystem;

/**
 * <p>Listener to receive notifications about changes on files within a watched directory and its sub-directories.</p>
 * <p>Note: implementations of this interface must be <em>thread-safe</em>.</p>
 */
public interface PathChangeListener {

    /**
     * <p>Setups the restriction object. That object will always be checked before any event is delivered to
     * {@link #modified(PathChangeEvent)}, {@link #supplement(DispatchKey, DispatchKey)}, or
     * {@link #discard(DispatchKey)}. This method will be called asynchronously for every detected
     * {@link java.nio.file.FileSystem}.</p>
     * <p>Note: Implementing this method is optional; the default method tells the restriction object to accept
     * anything.</p>
     *
     * @param pRestriction Restriction object, never {@code null}.
     * @param pFileSystem  The file-system for which this restriction applies, never {@code null}
     */
    default void restrict(final DispatchRestriction pRestriction, final FileSystem pFileSystem) {
        pRestriction.acceptAll();
    }

    /**
     * Indicates, that a file (never a directory) has been modified. Modified means,
     * that the file has been created or updated.
     *
     * @param pEvent Event which represents the creation or update of a file, never {@code null}
     * @throws IOException Thrown, if processing of the modified file failed for some reason.
     */
    void modified(PathChangeEvent pEvent) throws IOException;

    /**
     * <p>Indicates, that the file denoted with the {@link DispatchKey} specified has been discarded for some reason
     * (file/directory has been deleted, watched directory is being unregistered etc.).
     *
     * @param pKey File-key of the discarded file or directory, never {@code null}
     */
    void discard(DispatchKey pKey);

    /**
     * <p>Informs this observer that the known key specified is being supplemented with the additional key
     * specified. It is guaranteed that this method is executed <em>before</em> {@link #modified(PathChangeEvent)} is
     * entered with the additional key specified.</p>
     * <p>
     * <p>Explanation: bundle A registers a watched directory with path "/A/B/C". Later, bundle B registers a watched directory
     * with path "/A". Both of this directories are located in the same file-system. This means, when absolute
     * path /A/B/C/foo/bar.txt had been changed, the observers would be informed twice, one time with relative path
     * "foo/bar.txt" and one time with relative path "B/C/foo/bar.txt". This could lead to disproportional memory
     * usage and worse performance because the observers would take and action multiple times on the same content. To
     * avoid this, an implementation class can implement this optional method to react properly on supplementing
     * keys.</p>
     *
     * @param pKnownKey      Key which has already been delivered to this observer, never {@code null}
     * @param pAdditionalKey Key which never has been delivered until now to this observer, and, which supplements
     *                       the known key specified, never {@code null}
     */
    default void supplement(DispatchKey pKnownKey, DispatchKey pAdditionalKey) {
        // Implementation of the method is optional

    }
}
