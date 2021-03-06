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

import ch.sourcepond.commons.smartswitch.api.SmartSwitchBuilderFactory;
import ch.sourcepond.io.checksum.api.ResourcesFactory;
import ch.sourcepond.io.fileobserver.api.KeyDeliveryHook;
import ch.sourcepond.io.fileobserver.api.PathChangeListener;
import ch.sourcepond.io.fileobserver.impl.directory.DirectoryFactory;
import ch.sourcepond.io.fileobserver.impl.dispatch.DefaultDispatchKeyFactory;
import ch.sourcepond.io.fileobserver.impl.fs.DedicatedFileSystem;
import ch.sourcepond.io.fileobserver.impl.fs.DedicatedFileSystemFactory;
import ch.sourcepond.io.fileobserver.impl.listener.EventDispatcher;
import ch.sourcepond.io.fileobserver.impl.listener.ListenerManager;
import ch.sourcepond.io.fileobserver.spi.RelocationObserver;
import ch.sourcepond.io.fileobserver.spi.WatchedDirectory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.lang.String.format;
import static java.nio.file.Files.isDirectory;
import static java.util.Objects.requireNonNull;
import static org.osgi.service.component.annotations.ReferenceCardinality.MULTIPLE;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.slf4j.LoggerFactory.getLogger;

/**
 *
 */
@Component(service = {}, immediate = true)
@Designate(ocd = Config.class)
public class VirtualRoot implements RelocationObserver {
    private static final Logger LOG = getLogger(VirtualRoot.class);
    private static final String KEY_IS_NULL = "Key is null";
    private static final String DIRECTORY_IS_NULL = "Directory is null";
    private static final String WATCHED_DIRECTORY_IS_NULL = "Watched directory is null";
    private final InitSwitch<WatchedDirectory> rootInitSwitch = new InitSwitch<>(this::doAddRoot);
    private final InitSwitch<PathChangeListener> observerInitSwitch = new InitSwitch<>(this::doAddListener);
    private final InitSwitch<KeyDeliveryHook> hooksInitSwitch = new InitSwitch<>(this::doAddHook);
    private final ListenerManager manager;
    private final Map<Object, WatchedDirectory> watchedDirectories = new ConcurrentHashMap<>();
    private final ConcurrentMap<FileSystem, DedicatedFileSystem> children = new ConcurrentHashMap<>();
    private final DedicatedFileSystemFactory dedicatedFileSystemFactory;


    // Constructor for BundleActivator
    public VirtualRoot() {
        manager = new ListenerManager();
        final DefaultDispatchKeyFactory keyFactory = new DefaultDispatchKeyFactory();
        dedicatedFileSystemFactory = new DedicatedFileSystemFactory(
                new DirectoryFactory(keyFactory),
                manager);
    }

    // Constructor for testing
    public VirtualRoot(final DedicatedFileSystemFactory pDedicatedFileSystemFactory,
                       final ListenerManager pManager) {
        dedicatedFileSystemFactory = pDedicatedFileSystemFactory;
        manager = pManager;
    }

    @Activate
    public void activate(final Config pConfig) {
        setConfig(pConfig);
        rootInitSwitch.init();
        observerInitSwitch.init();
        hooksInitSwitch.init();
        LOG.info("Virtual-root activated");
    }

    @Deactivate
    public void deactivate() {
        children.values().forEach(DedicatedFileSystem::close);
        children.clear();
        dedicatedFileSystemFactory.shutdown();
        LOG.info("Virtual-root deactivated");
    }

    @Modified
    public void setConfig(final Config pConfig) {
        children.values().forEach(c -> c.setConfig(pConfig));
        dedicatedFileSystemFactory.setConfig(pConfig);
        manager.setConfig(pConfig);
    }

    @Reference
    public void setResourcesFactory(final ResourcesFactory pResourcesFactory) {
        dedicatedFileSystemFactory.setResourcesFactory(pResourcesFactory);
    }

    @Reference
    public void initExecutors(final SmartSwitchBuilderFactory pFactory) {
        final ExecutorService dispatcherExecutor = pFactory.newBuilder(ExecutorService.class).
                setFilter("(sourcepond.io.fileobserver.dispatcherexecutor=*)").
                setShutdownHook(ExecutorService::shutdown).
                build(Executors::newCachedThreadPool);
        final ExecutorService listenerExecutor = pFactory.newBuilder(ExecutorService.class).
                setFilter("(sourcepond.io.fileobserver.listenerexecutor=*)").
                setShutdownHook(ExecutorService::shutdown).
                build(Executors::newCachedThreadPool);
        manager.setExecutors(dispatcherExecutor, listenerExecutor);
        final ExecutorService directoryWalkerExecutor = pFactory.newBuilder(ExecutorService.class).
                setFilter("(sourcepond.io.fileobserver.directorywalkerexecutor=*)").
                setShutdownHook(ExecutorService::shutdown).
                build(Executors::newCachedThreadPool);
        dedicatedFileSystemFactory.setExecutors(directoryWalkerExecutor, dispatcherExecutor);
    }

    private void doAddListener(final PathChangeListener pListener) {
        final EventDispatcher dispatcher = manager.addListener(pListener);
        children.values().forEach(dfs -> dfs.forceInform(dispatcher));
    }

    /**
     * Whiteboard bind-method for {@link PathChangeListener} services exported by any client bundle. This
     * method is called when a client exports a service which implements the {@link PathChangeListener} interface.
     *
     * @param pListener File observer service to be registered.
     */
    @Reference(policy = DYNAMIC, cardinality = MULTIPLE)
    public void addListener(final PathChangeListener pListener) {
        requireNonNull(pListener, "Observer is null");
        observerInitSwitch.add(pListener);
    }

    /**
     * Whiteboard unbind-method {@link PathChangeListener} services exported by any client bundle. This method is
     * called when a client unregisters a service which implements the {@link PathChangeListener} interface.
     *
     * @param pObserver File observer service to be unregistered.
     */
    public void removeListener(final PathChangeListener pObserver) {
        requireNonNull(pObserver, "Observer is null");
        manager.removeObserver(pObserver);
    }

    private DedicatedFileSystem newDedicatedFileSystem(final FileSystem pFs) {
        try {
            return dedicatedFileSystemFactory.openFileSystem(this, pFs);
        } catch (final IOException e) {
            throw new UncheckedIOException(e.getMessage(), e);
        }
    }

    private void doAddHook(final KeyDeliveryHook pHook) {
        manager.addHook(pHook);
    }

    @Reference(policy = DYNAMIC, cardinality = MULTIPLE)
    public void addHook(final KeyDeliveryHook pHook) {
        requireNonNull(pHook, "Hook is null");
        hooksInitSwitch.add(pHook);
    }

    public void removeHook(final KeyDeliveryHook pHook) {
        manager.removeHook(pHook);
    }

    // This method must be synchronized because all sub-directories need to be
    // registered before another WatchedDirectory is being registered.
    private synchronized void doAddRoot(final WatchedDirectory pWatchedDirectory) {
        final Object key = requireNonNull(pWatchedDirectory.getKey(), KEY_IS_NULL);
        final Path directory = requireNonNull(pWatchedDirectory.getDirectory(), DIRECTORY_IS_NULL);

        if (!isDirectory(directory)) {
            throw new IllegalArgumentException(format("[%s]: %s is not a directory!", key, directory));
        }

        // Insure that the directory-key is unique
        if (watchedDirectories.containsKey(key)) {
            throw new IllegalArgumentException(format("Key %s already used by %s", key, watchedDirectories.get(key)));
        }
        watchedDirectories.put(key, pWatchedDirectory);

        try {
            children.computeIfAbsent(directory.getFileSystem(),
                    this::newDedicatedFileSystem).registerRootDirectory(pWatchedDirectory);
            pWatchedDirectory.addObserver(this);
            LOG.info("Added [{}:{}]", key, directory);
        } catch (final IOException | UncheckedIOException e) {
            LOG.warn(e.getMessage(), e);
        }
    }

    /**
     * Whiteboard bind-method for {@link WatchedDirectory} services exported by any client bundle. This
     * method is called when a client exports a service which implements the {@link WatchedDirectory} interface.
     *
     * @param pWatchedDirectory Watched-directory service to be registered.
     */
    @Reference(policy = DYNAMIC, cardinality = MULTIPLE)
    public void addRoot(final WatchedDirectory pWatchedDirectory) {
        requireNonNull(pWatchedDirectory, WATCHED_DIRECTORY_IS_NULL);
        rootInitSwitch.add(pWatchedDirectory);
    }

    /**
     * Whiteboard unbind-method {@link WatchedDirectory} services exported by any client bundle. This method is
     * called when a client unregisters a service which implements the {@link WatchedDirectory} interface.
     *
     * @param pWatchedDirectory Watched-directory service to be unregistered.
     */
    // This method must be synchronized because all sub-directories need to be
    // discarded before another WatchedDirectory is being unregistered.
    public synchronized void removeRoot(final WatchedDirectory pWatchedDirectory) {
        requireNonNull(pWatchedDirectory, WATCHED_DIRECTORY_IS_NULL);
        final Object key = requireNonNull(pWatchedDirectory.getKey(), KEY_IS_NULL);
        final Path directory = requireNonNull(pWatchedDirectory.getDirectory(), DIRECTORY_IS_NULL);

        // It's already checked that nothing is null
        final DedicatedFileSystem fs = children.get(directory.getFileSystem());

        if (fs == null) {
            LOG.warn(format("No dedicated file system registered! Path: %s", directory));
        } else {
            fs.unregisterRootDirectory(pWatchedDirectory.getDirectory(), pWatchedDirectory);

            // IMPORTANT: remove watched-directory with key specified.
            watchedDirectories.remove(key);
            pWatchedDirectory.removeObserver(this);
            LOG.info("Removed [{}:{}]", key, directory);
        }
    }

    /**
     *
     */
    @Override
    public synchronized void destinationChanged(final WatchedDirectory pWatchedDirectory, final Path pPrevious) throws IOException {
        requireNonNull(pWatchedDirectory, WATCHED_DIRECTORY_IS_NULL);
        requireNonNull(pPrevious, "Previous directory is null");
        final Object key = requireNonNull(pWatchedDirectory.getKey(), KEY_IS_NULL);
        final Path directory = requireNonNull(pWatchedDirectory.getDirectory(), DIRECTORY_IS_NULL);

        if (watchedDirectories.replace(key, pWatchedDirectory) != null) {
            if (pPrevious.equals(directory)) {
                LOG.info("Nothing changed; skipped destination change for {}", pPrevious);
            } else {
                getDedicatedFileSystem(directory).destinationChanged(
                        pWatchedDirectory, pPrevious);
            }
        } else {
            LOG.warn("Directory with key {} was not mapped; nothing changed", directory);
        }
    }

    private DedicatedFileSystem getDedicatedFileSystem(final Path pPath) {
        final DedicatedFileSystem fsdirs = children.get(pPath.getFileSystem());
        if (null == fsdirs) {
            throw new IllegalStateException(format("No appropriate root-directory found for %s", pPath));
        }
        return fsdirs;
    }

    /**
     * Removes the {@link DedicatedFileSystem} instance specified from
     * this virtual root. This happens when the {@link java.nio.file.WatchService} of the
     * fs specified had been closed and a {@link java.nio.file.ClosedWatchServiceException} has been
     * caused to be thrown.
     *
     * @param pDedicatedFileSystem
     */
    public void removeFileSystem(final DedicatedFileSystem pDedicatedFileSystem) {
        for (final Iterator<Map.Entry<FileSystem, DedicatedFileSystem>> it = children.entrySet().iterator() ; it.hasNext() ; ) {
            final Map.Entry<FileSystem, DedicatedFileSystem> next = it.next();
            if (pDedicatedFileSystem.equals(next.getValue())) {
                manager.removeFileSystem(next.getKey());
                it.remove();
            }
        }
    }
}
