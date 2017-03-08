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

import ch.sourcepond.commons.smartswitch.lib.SmartSwitchActivatorBase;
import ch.sourcepond.io.checksum.api.ResourcesFactory;
import ch.sourcepond.io.fileobserver.api.FileObserver;
import ch.sourcepond.io.fileobserver.impl.directory.DirectoryFactory;
import ch.sourcepond.io.fileobserver.impl.directory.DirectoryScanner;
import ch.sourcepond.io.fileobserver.impl.fs.VirtualRoot;
import ch.sourcepond.io.fileobserver.spi.WatchedDirectory;
import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.lang.String.format;
import static java.nio.file.Files.isDirectory;
import static java.time.Clock.systemUTC;
import static java.util.Objects.requireNonNull;

/**
 * Bundle activator; this class manages the lifecycle of the bundle.
 */
public class Activator extends SmartSwitchActivatorBase {
    private static final Logger LOG = LoggerFactory.getLogger(Activator.class);
    private final ExecutorServices executorServices;
    private final DirectoryFactory directoryFactory;
    private final VirtualRoot virtualRoot;
    private final DirectoryScanner directoryScanner;

    // Constructor for OSGi framework
    public Activator() {
        executorServices = new ExecutorServices();
        directoryFactory = new DirectoryFactory(executorServices);
        virtualRoot = new VirtualRoot(directoryFactory);
        directoryScanner = new DirectoryScanner(systemUTC(), virtualRoot);
    }

    // Constructor for testing
    public Activator(final ExecutorServices pExecutorServices,
                     final DirectoryFactory pDirectoryFactory,
                     final VirtualRoot pVirtualRoot,
                     final DirectoryScanner pDirectoryScanner) {
        executorServices = pExecutorServices;
        directoryFactory = pDirectoryFactory;
        virtualRoot = pVirtualRoot;
        directoryScanner = pDirectoryScanner;
    }

    @Override
    public void destroy(final BundleContext context, final DependencyManager manager) throws Exception {
        super.destroy(context, manager);
        directoryScanner.stop();
        virtualRoot.stop();
    }

    @Override
    public void init(final BundleContext bundleContext, final DependencyManager dependencyManager) throws Exception {
        dependencyManager.add(createComponent().
                setImplementation(executorServices).
                add(createSmartSwitchBuilder(ExecutorService.class).
                        setFilter("(sourcepond.io.fileobserver.observerexecutor=*)").
                        setShutdownHook(e -> e.shutdown()).
                        build(() -> Executors.newWorkStealingPool(5)
                        ).setAutoConfig("observerExecutor")).
                add(createSmartSwitchBuilder(ExecutorService.class).
                        setFilter("(sourcepond.io.fileobserver.directorywalkerexecutor=*)").
                        setShutdownHook(e -> e.shutdown()).
                        build(() -> Executors.newCachedThreadPool()).setAutoConfig("directoryWalkerExecutor")
                ));
        dependencyManager.add(createComponent().
                setImplementation(directoryFactory).
                add(createServiceDependency().
                        setService(ResourcesFactory.class).
                        setRequired(true)
                ));
        dependencyManager.add(createComponent().
                setImplementation(virtualRoot).
                add(createServiceDependency().
                        setService(FileObserver.class).
                        setCallbacks("addObserver", "removeObserver")
                ));
        dependencyManager.add(createComponent().setImplementation(directoryScanner));
        dependencyManager.add(createComponent().
                setImplementation(this).
                add(createServiceDependency().
                        setService(WatchedDirectory.class).
                        setCallbacks("bind", "unbind")
                ));
    }

    /**
     * Whiteboard bind-method for {@link WatchedDirectory} services exported by any client bundle. This
     * method is called when a client exports a service which implements the {@link WatchedDirectory} interface.
     *
     * @param pWatchedDirectory Watched-directory service to be registered.
     * @throws IOException Thrown, if the root directory could not be added.
     */
    public void bind(final WatchedDirectory pWatchedDirectory) throws IOException {
        requireNonNull(pWatchedDirectory, "Watched directory is null");
        final Object key = requireNonNull(pWatchedDirectory.getKey(), "Key is null");
        final Path directory = requireNonNull(pWatchedDirectory.getDirectory(), "Directory is null");

        if (!isDirectory(directory)) {
            throw new IllegalArgumentException(format("[%s]: %s is not a directory!", key, directory));
        }
        virtualRoot.addRoot(pWatchedDirectory);

        if (LOG.isInfoEnabled()) {
            LOG.info("Added watched-directory with directory-key %s and path %s", key, directory);
        }
    }

    /**
     * Whiteboard unbind-method {@link WatchedDirectory} services exported by any client bundle. This method is
     * called when a client unregisters a service which implements the {@link WatchedDirectory} interface.
     *
     * @param pWatchedDirectory Watched-directory service to be unregistered.
     */
    public void unbind(final WatchedDirectory pWatchedDirectory) {
        requireNonNull(pWatchedDirectory, "Watched directory is null");
        final Object key = requireNonNull(pWatchedDirectory.getKey(), "Key is null");
        final Path directory = requireNonNull(pWatchedDirectory.getDirectory(), "Directory is null");
        virtualRoot.removeRoot(pWatchedDirectory);

        if (LOG.isInfoEnabled()) {
            LOG.info("Removed watched-directory with directory-key %s and path %s", key, directory);
        }
    }
}
