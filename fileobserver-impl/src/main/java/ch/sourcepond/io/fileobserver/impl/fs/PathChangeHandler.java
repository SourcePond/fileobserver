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
package ch.sourcepond.io.fileobserver.impl.fs;

import ch.sourcepond.io.fileobserver.impl.VirtualRoot;
import ch.sourcepond.io.fileobserver.impl.directory.Directory;
import ch.sourcepond.io.fileobserver.impl.listener.EventDispatcher;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import static java.lang.String.format;
import static java.nio.file.Files.isDirectory;
import static java.nio.file.Files.isRegularFile;
import static java.util.Objects.requireNonNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 *
 */
class PathChangeHandler {
    private static final Logger LOG = getLogger(PathChangeHandler.class);
    private final VirtualRoot virtualRoot;
    private final DirectoryRegistrationWalker walker;
    private final ConcurrentMap<Path, Directory> dirs;

    PathChangeHandler(final VirtualRoot pVirtualRoot,
                      final DirectoryRegistrationWalker pWalker,
                      final ConcurrentMap<Path, Directory> pDirs) {
        virtualRoot = pVirtualRoot;
        walker = pWalker;
        dirs = pDirs;
    }

    void rootAdded(final EventDispatcher pDispatcher, final Directory pNewRoot) {
        walker.rootAdded(pDispatcher, pNewRoot);
    }

    void removeFileSystem(final DedicatedFileSystem pDfs) {
        virtualRoot.removeFileSystem(pDfs);
    }

    private Directory getDirectory(final Path pPath) {
        return dirs.get(pPath);
    }

    void pathModified(final EventDispatcher pDispatcher,
                      final Path pPath,
                      final Runnable pDoneCallback,
                      final boolean pIsCreated) {
        if (isDirectory(pPath)) {
            //if (pIsCreated) {
                walker.directoryCreated(pDispatcher, pPath, pDoneCallback);
            //} else {
              //  pDoneCallback.run();
           // }
        } else {
            final Directory dir = requireNonNull(getDirectory(pPath.getParent()),
                    () -> format("No directory registered for file %s", pPath));
            dir.informIfChanged(pDispatcher, pPath, pDoneCallback, pIsCreated);
        }
    }

    void pathDiscarded(final EventDispatcher pDispatcher,
                       final Path pPath,
                       final Runnable pDoneCallback) {
        // The deleted path was a directory
        if (!directoryDiscarded(pDispatcher, pPath, pDoneCallback)) {
            final Directory parentDirectory = getDirectory(pPath.getParent());
            if (parentDirectory == null) {
                LOG.warn("Parent of {} does not exist. Nothing to discard", pPath, new Exception());
                pDoneCallback.run();
            } else {
                // The deleted path was a file
                parentDirectory.informDiscard(pDispatcher, pPath, pDoneCallback);
            }
        }
    }

    private boolean directoryDiscarded(final EventDispatcher pDispatcher,
                                       final Path pDirectory,
                                       final Runnable pDoneCallback) {
        final Directory dir = dirs.remove(pDirectory);
        final boolean wasDirectory = dir != null;
        if (wasDirectory) {
            dir.cancelKey();
            for (final Iterator<Map.Entry<Path, Directory>> it = dirs.entrySet().iterator(); it.hasNext(); ) {
                final Map.Entry<Path, Directory> entry = it.next();
                if (entry.getKey().startsWith(pDirectory)) {
                    entry.getValue().cancelKey();
                    it.remove();
                }
            }
            dir.informDiscard(pDispatcher, pDirectory, pDoneCallback);
        }
        return wasDirectory;
    }
}
