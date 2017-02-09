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
 * Observer interface to receive notifications about changes on a watched (file-) paths.
 */
public interface FileObserver {

    Enum<?>[] getKeys();

    /**
     * Checks whether this observer should handle the path specified. When this method
     * returns {@code true}, {@link #modified(String, Path)} will be called.
     *
     * @param pFile Path of the modified file, never {@code null}.
     * @return {@code true} if the path should be dispatched, {@code false}
     * otherwise.
     */
    boolean accept(String pId, Path pFile);

    void modified(String pId, Path pFile);

    void deleted(String pId);
}
