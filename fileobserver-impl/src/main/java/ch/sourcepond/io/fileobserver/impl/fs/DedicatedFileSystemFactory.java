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

import ch.sourcepond.io.checksum.api.ResourcesFactory;
import ch.sourcepond.io.fileobserver.impl.Config;
import ch.sourcepond.io.fileobserver.impl.VirtualRoot;
import ch.sourcepond.io.fileobserver.impl.directory.Directory;
import ch.sourcepond.io.fileobserver.impl.directory.DirectoryFactory;
import ch.sourcepond.io.fileobserver.impl.listener.ListenerManager;
import ch.sourcepond.io.fileobserver.impl.pending.PendingEventRegistry;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

/**
 *
 */
public class DedicatedFileSystemFactory {
    private final DirectoryFactory directoryFactory;
    private final ListenerManager dispatcher;

    // Injected by SCR
    private volatile Executor directoryWalkerExecutor;

    // Constructor for BundleActivator
    public DedicatedFileSystemFactory(final DirectoryFactory pDirectoryFactory, final ListenerManager pDispatcher) {
        directoryFactory = pDirectoryFactory;
        dispatcher = pDispatcher;
    }

    // Constructor for testing
    public DedicatedFileSystemFactory(final DirectoryFactory pDirectoryFactory,
                                      final ListenerManager pDispatcher,
                                      final ExecutorService pDirectoryWalkerExecutor) {
        directoryFactory = pDirectoryFactory;
        dispatcher = pDispatcher;
        directoryWalkerExecutor = pDirectoryWalkerExecutor;
    }

    public void setResourcesFactory(final ResourcesFactory pResourcesFactory) {
        directoryFactory.setResourcesFactory(pResourcesFactory);
    }

    public void setDirectoryWalkerExecutor(final Executor pDirectoryWalkerExecutor) {
        directoryFactory.setDirectoryWalkerExecutor(pDirectoryWalkerExecutor);
        directoryWalkerExecutor = pDirectoryWalkerExecutor;
    }

    public DedicatedFileSystem openFileSystem(final VirtualRoot pVirtualRoot, final FileSystem pFs, final PendingEventRegistry pPendingEventRegistry) throws IOException {
        final ConcurrentMap<Path, Directory> dirs = new ConcurrentHashMap<>();
        final WatchServiceWrapper wrapper = new WatchServiceWrapper(pFs);
        final DirectoryRegistrationWalker walker = new DirectoryRegistrationWalker(
                wrapper,
                directoryFactory,
                directoryWalkerExecutor,
                dirs);
        DedicatedFileSystem fs = new DedicatedFileSystem(
                pPendingEventRegistry,
                directoryFactory,
                wrapper,
                new DirectoryRebase(directoryFactory, wrapper, dirs),
                dispatcher,
                new PathChangeHandler(pVirtualRoot, walker, dirs),
                dirs);
        fs.start();
        return fs;
    }

    public void setListenerExecutor(final ExecutorService pExecutor) {
        directoryFactory.setListenerExecutor(pExecutor);
    }

    public void setConfig(final Config pConfig) {
        directoryFactory.setConfig(pConfig);
    }
}
