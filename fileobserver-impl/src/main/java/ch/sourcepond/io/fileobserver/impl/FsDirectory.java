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
package ch.sourcepond.io.fileobserver.impl;

import org.slf4j.Logger;

import java.nio.file.Path;
import java.nio.file.WatchKey;

import static org.slf4j.LoggerFactory.getLogger;

/**
 *
 */
class FsDirectory  {
    private static final Logger LOG = getLogger(FsDirectory.class);
    private final FsDirectory parent;
    private final WatchKey key;

    FsDirectory(final FsDirectory pParent, final WatchKey pKey) {
        parent = pParent;
        key = pKey;
    }

    Path getPath() {
        return (Path)key.watchable();
    }

    private Path findRoot() {
        Path rootDir = getPath();
        FsDirectory dir = parent;
        while (null != dir) {
            rootDir = dir.getPath();
            dir = dir.parent;
        }
        return rootDir;
    }

    void cancelKey() {
        key.cancel();
    }

    String relativize(final Path pFile) {
        return findRoot().relativize(pFile).toString();
    }
}
