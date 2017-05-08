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

import ch.sourcepond.commons.smartswitch.api.SmartSwitchBuilder;
import ch.sourcepond.commons.smartswitch.api.SmartSwitchBuilderFactory;
import ch.sourcepond.io.checksum.api.ResourcesFactory;
import ch.sourcepond.io.fileobserver.api.KeyDeliveryHook;
import ch.sourcepond.io.fileobserver.api.PathChangeListener;
import ch.sourcepond.io.fileobserver.impl.fs.DedicatedFileSystem;
import ch.sourcepond.io.fileobserver.impl.fs.DedicatedFileSystemFactory;
import ch.sourcepond.io.fileobserver.impl.listener.EventDispatcher;
import ch.sourcepond.io.fileobserver.impl.listener.ListenerManager;
import ch.sourcepond.io.fileobserver.impl.pending.PendingEventRegistry;
import ch.sourcepond.io.fileobserver.spi.WatchedDirectory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.spi.FileSystemProvider;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.*;

/**
 *
 */
public class VirtualRootTest {
    private static final long MODIFICATION_LOCKING_TIME = 1000L;
    private static final Object ROOT_KEY = new Object();
    private static final Object OTHER_KEY = new Object();
    private final Config config = mock(Config.class);
    private final WatchedDirectory watchedDir = mock(WatchedDirectory.class);
    private final FileSystem fs = mock(FileSystem.class);
    private final FileSystemProvider provider = mock(FileSystemProvider.class);
    private final BasicFileAttributes attrs = mock(BasicFileAttributes.class);
    private final Path directory = mock(Path.class);
    private final BasicFileAttributes modifiedPathAttrs = mock(BasicFileAttributes.class);
    private final Path modifiedPath = mock(Path.class);
    private final PathChangeListener listener = mock(PathChangeListener.class);
    private final KeyDeliveryHook hook = mock(KeyDeliveryHook.class);
    private final ResourcesFactory resourcesFactory = mock(ResourcesFactory.class);
    private final DedicatedFileSystem dedicatedFs = mock(DedicatedFileSystem.class);
    private final DedicatedFileSystemFactory dedicatedFsFactory = mock(DedicatedFileSystemFactory.class);
    private final SmartSwitchBuilderFactory ssbFactory = mock(SmartSwitchBuilderFactory.class);
    private final SmartSwitchBuilder<ExecutorService> executorBuilder = mock(SmartSwitchBuilder.class);
    private final ListenerManager manager = mock(ListenerManager.class);
    private final EventDispatcher dispatcher = mock(EventDispatcher.class);
    private final PendingEventRegistry pendingEventRegistry = mock(PendingEventRegistry.class);
    private ExecutorService dispatcherExecutor;
    private ExecutorService listenerExecutor;
    private ExecutorService directoryWalkerExecutor;
    private VirtualRoot virtualRoot = new VirtualRoot(dedicatedFsFactory, manager, pendingEventRegistry);

    @Before
    public void setup() throws IOException {
        when(config.modificationLockingTime()).thenReturn(MODIFICATION_LOCKING_TIME);
        when(manager.addListener(listener)).thenReturn(dispatcher);
        when(modifiedPath.getFileSystem()).thenReturn(fs);
        when(provider.readAttributes(modifiedPath, BasicFileAttributes.class)).thenReturn(modifiedPathAttrs);

        when(directory.getFileSystem()).thenReturn(fs);
        when(fs.provider()).thenReturn(provider);
        when(provider.readAttributes(directory, BasicFileAttributes.class)).thenReturn(attrs);
        when(attrs.isDirectory()).thenReturn(true);

        when(watchedDir.getKey()).thenReturn(ROOT_KEY);
        when(watchedDir.getDirectory()).thenReturn(directory);
        when(dedicatedFsFactory.openFileSystem(virtualRoot, fs, pendingEventRegistry)).thenReturn(dedicatedFs);

        virtualRoot.addRoot(watchedDir);
        virtualRoot.addListener(listener);
        virtualRoot.activate(config);
    }

    @Test
    public void setResourcesFactory() {
        virtualRoot.setResourcesFactory(resourcesFactory);
        verify(dedicatedFsFactory).setResourcesFactory(resourcesFactory);
    }

    @Test
    public void setConfig() {
        verify(dedicatedFsFactory).setConfig(config);
        verify(pendingEventRegistry).setModificationLockingTime(MODIFICATION_LOCKING_TIME);
        verify(manager).setConfig(config);
    }

    private void setupDefaultExecutor(final String pFilter, final SmartSwitchBuilder<ExecutorService> pBuilder, final Answer<ExecutorService> pAnswer) {
        when(ssbFactory.newBuilder(ExecutorService.class)).thenReturn(executorBuilder);
        when(executorBuilder.setFilter(pFilter)).thenReturn(pBuilder);
        when(pBuilder.setShutdownHook(notNull())).thenReturn(pBuilder);
        when(pBuilder.build(notNull())).thenAnswer(pAnswer);
    }

    @Test
    public void initExecutors() {
        final SmartSwitchBuilder<ExecutorService> dispatcherExecutorBuilder = mock(SmartSwitchBuilder.class);
        setupDefaultExecutor("(sourcepond.io.fileobserver.dispatcherexecutor=*)", dispatcherExecutorBuilder, inv -> {
            final Supplier<ExecutorService> s = inv.getArgument(0);
            dispatcherExecutor = s.get();
            assertNotNull(dispatcherExecutor);
            return dispatcherExecutor;
        });
        final SmartSwitchBuilder<ExecutorService> listenerExecutorBuilder = mock(SmartSwitchBuilder.class);
        setupDefaultExecutor("(sourcepond.io.fileobserver.listenerexecutor=*)", listenerExecutorBuilder, inv -> {
            final Supplier<ExecutorService> s = inv.getArgument(0);
            listenerExecutor = s.get();
            assertNotNull(listenerExecutor);
            return listenerExecutor;
        });
        final SmartSwitchBuilder<ExecutorService> directoryWalkerExecutorBuilder = mock(SmartSwitchBuilder.class);
        setupDefaultExecutor("(sourcepond.io.fileobserver.directorywalkerexecutor=*)", directoryWalkerExecutorBuilder, inv -> {
            final Supplier<ExecutorService> s = inv.getArgument(0);
            directoryWalkerExecutor = s.get();
            assertNotNull(directoryWalkerExecutor);
            return directoryWalkerExecutor;
        });
        virtualRoot.initExecutors(ssbFactory);
        verify(manager).setExecutors(dispatcherExecutor, listenerExecutor);
        verify(dedicatedFsFactory).setExecutors(directoryWalkerExecutor, listenerExecutor);
    }

    @Test
    public void verifyActivatorConstructor() {
        new VirtualRoot();
    }

    @Test(expected = NullPointerException.class)
    public void addRootWatchedDirectoryIsNull() throws IOException {
        virtualRoot.addRoot(null);
    }

    @Test(expected = NullPointerException.class)
    public void addRootKeyIsNull() throws IOException {
        when(watchedDir.getKey()).thenReturn(null);
        virtualRoot.addRoot(watchedDir);
    }

    @Test(expected = NullPointerException.class)
    public void addRootDirectoryIsNull() throws IOException {
        when(watchedDir.getDirectory()).thenReturn(null);
        virtualRoot.addRoot(watchedDir);
    }

    @Test
    public void addRoot() throws IOException {
        verify(dedicatedFs).registerRootDirectory(same(watchedDir));
        verify(watchedDir).addObserver(virtualRoot);
    }

    @Test
    public void addRootDirectoriesCouldNotBeCreated() throws IOException {
        virtualRoot = new VirtualRoot(dedicatedFsFactory, manager, pendingEventRegistry);
        doThrow(IOException.class).when(dedicatedFsFactory).openFileSystem(virtualRoot, fs, pendingEventRegistry);

        // This should not cause an exception
        virtualRoot.addRoot(watchedDir);
    }

    @Test(expected = NullPointerException.class)
    public void addListenerIsNull() {
        virtualRoot.addListener(null);
    }

    @Test
    public void addHook() {
        virtualRoot.addHook(hook);
        verify(manager).addHook(hook);
    }

    @Test
    public void removeHook() {
        virtualRoot.removeHook(hook);
        verify(manager).removeHook(hook);
    }

    @Test
    public void addListener() {
        virtualRoot.removeListener(listener);
        verify(dedicatedFs).forceInform(dispatcher);
    }

    @Test
    public void removeListener() throws IOException {
        reset(dedicatedFs);
        when(watchedDir.getKey()).thenReturn(OTHER_KEY);
        virtualRoot.removeListener(listener);
        virtualRoot.addRoot(watchedDir);
        verify(dedicatedFs).registerRootDirectory(same(watchedDir));
    }

    @Test(expected = IllegalArgumentException.class)
    public void addRootPathIsNotADirectory() throws IOException {
        when(watchedDir.getKey()).thenReturn(OTHER_KEY);
        when(attrs.isDirectory()).thenReturn(false);
        virtualRoot.addRoot(watchedDir);
    }

    @Test(expected = IllegalArgumentException.class)
    public void addWatchedDirectoryWithSameKeyTwiceIsNotAllowed() throws IOException {
        virtualRoot.addRoot(watchedDir);
    }

    @Test(expected = NullPointerException.class)
    public void removeRootWatchedDirectoryIsNull() throws IOException {
        virtualRoot.removeRoot(null);
    }

    @Test(expected = NullPointerException.class)
    public void removeRootKeyIsNull() throws IOException {
        when(watchedDir.getKey()).thenReturn(null);
        virtualRoot.removeRoot(watchedDir);
    }

    @Test(expected = NullPointerException.class)
    public void removeRootDirectoryIsNull() throws IOException {
        when(watchedDir.getDirectory()).thenReturn(null);
        virtualRoot.removeRoot(watchedDir);
    }

    @Test
    public void removeRootNoSuchDirectoryRegistered() throws IOException {
        virtualRoot = new VirtualRoot(dedicatedFsFactory, manager, pendingEventRegistry);

        // This should not cause an exception
        virtualRoot.removeRoot(watchedDir);
    }

    @Test
    public void removeRoot() throws IOException {
        virtualRoot.removeRoot(watchedDir);
        verify(dedicatedFs).unregisterRootDirectory(directory, watchedDir);
        verify(watchedDir).removeObserver(virtualRoot);

        // This should not cause an exception
        virtualRoot.addRoot(watchedDir);
    }

    @Test
    public void deactivate() {
        virtualRoot.deactivate();
        verify(dedicatedFs).close();
        final PathChangeListener otherListener = mock(PathChangeListener.class);
        virtualRoot.addListener(otherListener);
        verifyZeroInteractions(otherListener);
    }

    @Test
    public void removeFileSystem() {
        virtualRoot.removeFileSystem(dedicatedFs);
        final PathChangeListener otherListener = mock(PathChangeListener.class);
        verify(manager).removeFileSystem(fs);
        virtualRoot.addListener(otherListener);
        verifyZeroInteractions(otherListener);
    }

    @Test(expected = NullPointerException.class)
    public void destintationChangeWatchedDirectoryIsNull() throws IOException {
        virtualRoot.destinationChanged(null, directory);
    }

    @Test(expected = NullPointerException.class)
    public void destintationChangePreviousDirectoryIsNull() throws IOException {
        virtualRoot.destinationChanged(watchedDir, null);
    }

    @Test(expected = NullPointerException.class)
    public void destintationChangeKeyIsNull() throws IOException {
        when(watchedDir.getKey()).thenReturn(null);
        virtualRoot.destinationChanged(watchedDir, directory);
    }

    @Test(expected = NullPointerException.class)
    public void destintationChangeCurrentDirIsNull() throws IOException {
        when(watchedDir.getDirectory()).thenReturn(null);
        virtualRoot.destinationChanged(watchedDir, directory);
    }

    @Test
    public void destintationChangeDirectoryNotMapped() throws IOException {
        virtualRoot.removeRoot(watchedDir);

        // This should not cause an exception
        virtualRoot.destinationChanged(watchedDir, directory);
        verify(dedicatedFs, never()).destinationChanged(any(), any());
    }

    @Test
    public void destintationChangePreviousAndCurrentDirAreEqual() throws IOException {
        virtualRoot.destinationChanged(watchedDir, directory);
        verify(dedicatedFs, never()).destinationChanged(any(), any());
    }

    @Test
    public void destintationChange() throws IOException {
        final Path newDirectory = mock(Path.class);
        virtualRoot.destinationChanged(watchedDir, newDirectory);
        verify(dedicatedFs).destinationChanged(watchedDir, newDirectory);
    }

}
