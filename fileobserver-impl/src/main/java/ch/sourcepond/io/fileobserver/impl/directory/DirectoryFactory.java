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
import ch.sourcepond.io.checksum.api.ResourcesFactory;
import ch.sourcepond.io.fileobserver.api.FileKey;
import ch.sourcepond.io.fileobserver.impl.Config;
import ch.sourcepond.io.fileobserver.impl.filekey.DefaultFileKeyFactory;
import ch.sourcepond.io.fileobserver.spi.WatchedDirectory;

import java.nio.file.Path;
import java.nio.file.WatchKey;
import java.util.concurrent.Executor;

/**
 *
 */
public class DirectoryFactory {
    private final DefaultFileKeyFactory fileKeyFactory;
    private volatile Config config;

    // Injected by SCR
    private volatile Executor directoryWalkerExecutor;

    // Injected by SCR
    private volatile Executor observerExecutor;
    // Injected by SCR
    private volatile ResourcesFactory resourcesFactory;

    // Constructor for BundleActivator
    public DirectoryFactory(final DefaultFileKeyFactory pFileKeyFactory) {
        fileKeyFactory = pFileKeyFactory;
    }

    public void setConfig(final Config pConfig) {
        config = pConfig;
    }

    public void setDirectoryWalkerExecutor(final Executor pDirectoryWalkerExecutor) {
        directoryWalkerExecutor = pDirectoryWalkerExecutor;
    }

    public void setObserverExecutor(final Executor pObserverExecutor) {
        observerExecutor = pObserverExecutor;
    }

    public void setResourcesFactory(final ResourcesFactory pResourcesFactory) {
        resourcesFactory = pResourcesFactory;
    }

    public RootDirectory newRoot(final WatchKey pWatchKey) {
        return new RootDirectory(this, pWatchKey);
    }

    public Directory newBranch(final Directory pParent, final WatchKey pKey) {
        return new SubDirectory(pParent, pKey);
    }

    /**
     * <p><em>INTERNAL API, only ot be used in class hierarchy</em></p>
     * <p>
     * Creates a new checksum {@link Resource} with the algorithm and file specified.
     *
     * @param pAlgorithm Algorithm, must not be {@code null}
     * @param pFile      File on which checksums shall be tracked, must not be {@code null}
     * @return New resource instance, never {@code null}
     */
    Resource newResource(final Algorithm pAlgorithm, final Path pFile) {
        return resourcesFactory.create(pAlgorithm, pFile);
    }

    /**
     * <p><em>INTERNAL API, only ot be used in class hierarchy</em></p>
     * <p>
     * Creates a new {@link FileKey} based on the directory-key and
     * relative path specified, see {@link Directory#addWatchedDirectory(WatchedDirectory)} for further information.
     *
     * @param pDirectoryKey Directory-key, must not be {@code null}
     * @param pRelativePath Relative path, must not be {@code null}
     * @return New file-key, never {@code null}
     */
    FileKey<?> newKey(final Object pDirectoryKey, final Path pRelativePath) {
        return fileKeyFactory.newKey(pDirectoryKey, pRelativePath);
    }

    /**
     * <p><em>INTERNAL API, only ot be used in class hierarchy</em></p>
     * <p>
     * Asynchronously executes the task specified using the observer executor service.
     *
     * @param pTask Task to be executed, must not be {@code null}
     */
    void executeObserverTask(final Runnable pTask) {
        observerExecutor.execute(pTask);
    }

    /**
     * <p><em>INTERNAL API, only ot be used in class hierarchy</em></p>
     * <p>
     * Asynchronously executes the task specified using the directory walker executor service.
     *
     * @param pTask Task to be executed, must not be {@code null}
     */
    void executeDirectoryWalkerTask(final Runnable pTask) {
        directoryWalkerExecutor.execute(pTask);
    }

    long getTimeout() {
        return config.timeout();
    }
}
