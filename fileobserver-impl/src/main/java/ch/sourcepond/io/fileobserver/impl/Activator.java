package ch.sourcepond.io.fileobserver.impl;

import ch.sourcepond.commons.smartswitch.lib.SmartSwitchActivatorBase;
import ch.sourcepond.io.checksum.api.ResourcesFactory;
import ch.sourcepond.io.fileobserver.api.FileObserver;
import ch.sourcepond.io.fileobserver.impl.directory.Directories;
import ch.sourcepond.io.fileobserver.impl.directory.DirectoryScanner;
import ch.sourcepond.io.fileobserver.impl.directory.FsDirectoryFactory;
import ch.sourcepond.io.fileobserver.impl.registrar.RegistrarFactory;
import ch.sourcepond.io.fileobserver.spi.WatchedDirectory;
import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.concurrent.*;

import static java.lang.String.format;
import static java.nio.file.Files.isDirectory;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

/**
 * Created by rolandhauser on 21.02.17.
 */
public class Activator extends SmartSwitchActivatorBase {
    private static final Logger LOG = LoggerFactory.getLogger(Activator.class);
    private final ConcurrentMap<Object, Path> keyToPaths = new ConcurrentHashMap<>();
    private final ConcurrentMap<Path, Collection<Object>> pathToKeys = new ConcurrentHashMap<>();
    private final ExecutorServices executorServices;
    private final FsDirectoryFactory fsDirectoryFactory;
    private final RegistrarFactory registrarFactory;
    private final Directories directories;
    private final DirectoryScanner directoryScanner;

    // Constructor for OSGi framework
    public Activator() {
        executorServices = new ExecutorServices();
        fsDirectoryFactory = new FsDirectoryFactory(executorServices);
        registrarFactory = new RegistrarFactory(executorServices, fsDirectoryFactory);
        directories = new Directories(executorServices, fsDirectoryFactory);
        directoryScanner = new DirectoryScanner(directories);
    }

    // Constructor for testing
    public Activator(final ExecutorServices pExecutorServices,
                     final FsDirectoryFactory pFsDirectoryFactory,
                     final RegistrarFactory pRegistrarFactory,
                     final Directories pDirectories,
                     final DirectoryScanner pDirectoryScanner) {
        executorServices = pExecutorServices;
        fsDirectoryFactory = pFsDirectoryFactory;
        registrarFactory = pRegistrarFactory;
        directories = pDirectories;
        directoryScanner = pDirectoryScanner;
    }

    @Override
    public void destroy(final BundleContext context, final DependencyManager manager) throws Exception {
        super.destroy(context, manager);
        directoryScanner.stop();
        directories.stop();
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
                setImplementation(fsDirectoryFactory).
                add(createServiceDependency().
                        setService(ResourcesFactory.class).
                        setRequired(true)
                ));
        dependencyManager.add(createComponent().
                setImplementation(directories).
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

    public void bind(final WatchedDirectory pWatchedDirectory) {
        if (null == pWatchedDirectory) {
            LOG.warn("Watched directory is null; nothing to bind");
        } else {
            try {
                final Object key = pWatchedDirectory.getKey();
                final Path directory = pWatchedDirectory.getDirectory();
                requireNonNull(key, "Key is null");
                requireNonNull(directory, "Directory is null");

                if (!isDirectory(directory)) {
                    throw new IllegalArgumentException(format("[%s]: %s is not a directory!", key, directory));
                }

                // Put the directory to the registered paths; if the returned value is null, it's a new key.
                final Path previous = keyToPaths.put(key, directory);

                if (!directory.equals(previous)) {
                    final Collection<Object> keys = pathToKeys.computeIfAbsent(directory, d -> new CopyOnWriteArraySet<>());

                    // If the key is newly added, open a watch-service for the directory
                    if (keys.add(key) && keys.size() == 1) {
                        directories.addRoot(key, directory);
                    }

                    // The previous directory is not null. This means, that we watch a new target
                    // directory, and therefore, need to clean-up.
                    disableIfNecessary(key, previous);
                }
            } catch (final IOException e) {
                LOG.warn(e.getMessage(), e);
            }
        }
    }

    public void unbind(final WatchedDirectory pWatchedDirectory) {
        if (null == pWatchedDirectory) {
            LOG.warn("Watched directory is null; nothing to unbind");
        } else {
            final Object key = pWatchedDirectory.getKey();
            requireNonNull(key, "Key is null");
            disableIfNecessary(key, keyToPaths.remove(key));
        }
    }

    private void disableIfNecessary(final Object pKey, final Path pToBeDisabled) {
        if (null != pToBeDisabled) {
            final Collection<Object> keys = pathToKeys.getOrDefault(pToBeDisabled, emptyList());

            // If no more keys are registered for the previous directory, its watch-key
            // needs to be cancelled.
            if (keys.remove(pKey) && keys.isEmpty()) {
                directories.removeRoot(pToBeDisabled);
                pathToKeys.remove(pKey);
            }
        }
    }
}
