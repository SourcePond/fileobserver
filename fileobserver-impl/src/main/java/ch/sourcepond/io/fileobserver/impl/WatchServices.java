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

import java.io.IOException;
import java.nio.file.ClosedFileSystemException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;

/**
 *
 */
class WatchServices {
    private static final Logger LOG = getLogger(WatchServices.class);
    private final Map<FileSystem, WatchKeys> watchServices = new HashMap<>();
    private final WatchKeysFactory watchKeysFactory;

    WatchServices(final WatchKeysFactory pWatchKeysFactory) {
        watchKeysFactory = pWatchKeysFactory;
    }

    synchronized void shutdown() {
        for (final Iterator<WatchKeys> it = watchServices.values().iterator(); it.hasNext(); ) {
            try {
                it.next().shutdown();
            } finally {
                it.remove();
            }
        }
    }

    private WatchKeys getKeys(final FileSystem pFs) throws IOException {
        try {
            return watchServices.computeIfAbsent(pFs,
                    fs -> watchKeysFactory.createKeys(fs));
        } catch (final WatchServiceException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    synchronized void cancelWatchKey(final Path pDirectory) throws IOException {
        if (getKeys(pDirectory.getFileSystem()).cancelWatchKey(pDirectory).isEmpty()) {
            // Value can never be null here
            watchServices.remove(pDirectory.getFileSystem()).shutdown();
        }
    }

    synchronized void openWatchKey(final Path pDirectory) throws IOException {
        try {
            getKeys(pDirectory.getFileSystem()).openWatchKey(pDirectory);
        } catch (final ClosedFileSystemException e) {
            // Value can never be null here
            watchServices.remove(pDirectory.getFileSystem()).shutdown();
            throw e;
        }
    }
}
