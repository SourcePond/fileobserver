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

/**
 * A file key combines a watched root directory (see {@link #key()}) and relative path within
 * that directory (see {@link #relativePath()}) to a unique identifier. Use a file key to associate a resource
 * to a specific file.
 *
 * The reason why not only the relative path is used as unique file key is simple: the fileobserver implementation
 * must be able to watch more than one directory at once. This means that the same relative path possibly exists in
 * more than one watched root directory.
 *
 * Another question may rise why an {@link Object} identifies a watched directory instead of an (absolute) {@link Path}.
 * The answer is that it must be possible to relocate a watched root directory during runtime. If a path would be used
 * to identify a watched root directory, it would not be possible for a {@link FileObserver} to determine which
 * resource is associated with the relocated directory.
 */
public interface FileKey {

    /**
     * The key which represents a watched root directory.
     *
     * @return Key, never {@code null}
     */
    Object key();

    Path relativePath();

    boolean isSubKey(FileKey pOther);
}
