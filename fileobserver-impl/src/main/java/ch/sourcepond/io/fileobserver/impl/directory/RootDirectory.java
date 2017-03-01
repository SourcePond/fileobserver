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
package ch.sourcepond.io.fileobserver.impl.directory;

import ch.sourcepond.io.checksum.api.Algorithm;
import ch.sourcepond.io.checksum.api.Resource;
import ch.sourcepond.io.fileobserver.api.FileKey;

import java.nio.file.Path;
import java.nio.file.WatchKey;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Represents a root-directory i.e. a directory which has been registered to watched.
 */
public class RootDirectory extends Directory {
    private final Collection<Object> directoryKeys = new CopyOnWriteArraySet<>();
    private final DirectoryFactory factory;

    RootDirectory(final DirectoryFactory pFactory, final WatchKey pWatchKey) {
        super(pWatchKey);
        factory = pFactory;
    }

    @Override
    public void addDirectoryKey(final Object pDirectoryKey) {
        directoryKeys.add(pDirectoryKey);
    }

    @Override
    public boolean removeDirectoryKey(final Object pDirectoryKey) {
        final Collection<Object> keys = directoryKeys;
        keys.remove(pDirectoryKey);
        return keys.isEmpty();
    }

    @Override
    Collection<Object> getDirectoryKeys() {
        return directoryKeys;
    }

    @Override
    FileKey createKey(final Object pDirectoryKey, final Path pRelativePath) {
        return factory.newKey(pDirectoryKey, pRelativePath);
    }

    /*
     * Delegates the Resource creation to the factory which created this object.
     */
    @Override
    Resource createResource(final Algorithm pAlgorithm, final Path pFile) {
        return factory.newResource(pAlgorithm, pFile);
    }

    /*
     * Delegates the task execution to the factory which created this object.
     */
    @Override
    void execute(final Runnable pTask) {
        factory.execute(pTask);
    }

    /*
     * Relativizes the path specified against the path of this directory.
     */
    @Override
    Path relativizeAgainstRoot(final Path pPath) {
        return getPath().relativize(pPath);
    }
}
