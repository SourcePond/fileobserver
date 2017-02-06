package ch.sourcepond.io.fileobserver.impl;

import ch.sourcepond.io.fileobserver.api.ResourceObserver;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.spi.FileSystemProvider;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Created by rolandhauser on 06.02.17.
 */
public class DirectoriesTest {
    private static final String RELATIVE_PATH = "relativePath";
    private final FsDirectoriesFactory fsDirectoriesFactory = mock(FsDirectoriesFactory.class);
    private final FsDirectories fsDirectories = mock(FsDirectories.class);
    private final FsDirectory fsDirectory = mock(FsDirectory.class);
    private final ResourceObserver observer = mock(ResourceObserver.class);
    private final WatchService watchService = mock(WatchService.class);
    private final FileSystem fs = mock(FileSystem.class);
    private final Path rootDirectory = mock(Path.class);
    private final BasicFileAttributes rootDirectoryAttrs = mock(BasicFileAttributes.class);
    private final BasicFileAttributes attrs = mock(BasicFileAttributes.class);
    private final Path testPath = mock(Path.class);
    private final FileSystemProvider provider = mock(FileSystemProvider.class);
    private final WatchKeyProcessor processor = mock(WatchKeyProcessor.class);
    private final WatchKey watchKey = mock(WatchKey.class);
    private final RegistrarFactory registrarFactory = mock(RegistrarFactory.class);
    private final Registrar registrar = mock(Registrar.class);
    private final CompoundObserverHandler compoundObserverHandler = mock(CompoundObserverHandler.class);
    private Directories directories = new Directories(registrarFactory, compoundObserverHandler, fsDirectoriesFactory);

    @Before
    public void setup() throws IOException {
        when(rootDirectory.getFileSystem()).thenReturn(fs);
        when(testPath.getFileSystem()).thenReturn(fs);
        when(fs.newWatchService()).thenReturn(watchService);
        when(fsDirectoriesFactory.newDirectories(registrar)).thenReturn(fsDirectories);
        when(fs.provider()).thenReturn(provider);
        when(provider.readAttributes(rootDirectory, BasicFileAttributes.class)).thenReturn(rootDirectoryAttrs);
        when(rootDirectoryAttrs.isDirectory()).thenReturn(true);
        when(provider.readAttributes(testPath, BasicFileAttributes.class)).thenReturn(attrs);
        when(registrarFactory.newRegistrar(fs)).thenReturn(registrar);
        directories.addRoot(rootDirectory);
    }

    @Test
    public void addRootIOExceptionOccurred() throws IOException {
        directories = new Directories(registrarFactory, compoundObserverHandler, fsDirectoriesFactory);

        final IOException expected = new IOException();
        doThrow(expected).when(registrarFactory).newRegistrar(fs);
        try {
            directories.addRoot(rootDirectory);
            fail("Exception expected");
        } catch (final IOException e) {
            assertSame(expected, e.getCause().getCause());
        }
    }


    @Test
    public void addRootPathIsNotADirectory() throws IOException {
        directories = new Directories(registrarFactory, compoundObserverHandler, fsDirectoriesFactory);
        when(rootDirectoryAttrs.isDirectory()).thenReturn(false);

        try {
            directories.addRoot(rootDirectory);
            fail("Exception expected");
        } catch (final IllegalArgumentException e) {
            // noop
        }
    }

    @Test
    public void removeRoot() throws IOException {
        when(fsDirectories.directoryDeleted(rootDirectory)).thenReturn(true);
        directories.removeRoot(rootDirectory);
        directories.addRoot(rootDirectory);
        verify(registrarFactory, times(2)).newRegistrar(fs);
    }

    @Test
    public void addObserver() {
        when(fsDirectories.getDirectory(testPath)).thenReturn(fsDirectory);
        when(fsDirectory.relativize(testPath)).thenReturn(RELATIVE_PATH);
        directories.addObserver(observer);

        // This should not have an effect
        directories.addObserver(observer);
        directories.addObserver(observer);

        directories.pathCreated(testPath);
        verify(fsDirectory).informIfChanged(compoundObserverHandler, testPath);
    }

    @Test
    public void removeObserver() {
        directories.removeObserver(observer);
        verify(compoundObserverHandler).remove(observer);
    }

    @Test
    public void pathCreatedPathIsADirectory() throws Exception {
        when(attrs.isDirectory()).thenReturn(true);
        directories.addObserver(observer);
        directories.pathCreated(testPath);
        verify(fsDirectories).directoryCreated(testPath, compoundObserverHandler);
    }

    @Test
    public void pathDeleted() throws Exception {
        when(fsDirectories.getDirectory(testPath)).thenReturn(fsDirectory);
        when(fsDirectory.relativize(testPath)).thenReturn(RELATIVE_PATH);
        when(fsDirectories.directoryDeleted(testPath)).thenReturn(true);

        directories.addObserver(observer);
        directories.pathDeleted(testPath);
        verify(compoundObserverHandler).deleted(RELATIVE_PATH);

        // Root should still be the same
        directories.addRoot(rootDirectory);

        // Should have been called twice
        verify(registrarFactory, times(2)).newRegistrar(fs);
    }

    @Test
    public void close() throws Exception {
        directories.close();
        verify(fsDirectories).close();

        directories.addRoot(rootDirectory);

        // Should have been called twice
        verify(registrarFactory, times(2)).newRegistrar(fs);
    }

    @Test
    public void processFsEvents() throws Exception {
        directories.addRoot(rootDirectory);
        when(fsDirectories.poll()).thenReturn(watchKey);
        directories.processFsEvents(processor);
        verify(processor).processEvent(watchKey);
    }

    @Test
    public void processFsEventsNoKeyAvailable() throws Exception {
        directories.addRoot(rootDirectory);
        directories.processFsEvents(processor);
        verify(processor, never()).processEvent(watchKey);
    }

    @Test
    public void processFsEventsIOExceptionOccurred() throws Exception {
        final IOException expected = new IOException();
        doThrow(expected).when(processor).processEvent(watchKey);
        directories.addRoot(rootDirectory);
        when(fsDirectories.poll()).thenReturn(watchKey);
        directories.processFsEvents(processor);

        directories.addRoot(rootDirectory);

        // Should have been called only once
        verify(registrarFactory).newRegistrar(fs);
    }

    @Test
    public void closeFsDirectoriesIfWatchServiceIsClosed() throws Exception {
        final ClosedWatchServiceException expected = new ClosedWatchServiceException();
        doThrow(expected).when(processor).processEvent(watchKey);
        directories.addRoot(rootDirectory);
        when(fsDirectories.poll()).thenReturn(watchKey);
        directories.processFsEvents(processor);

        verify(fsDirectories).close();

        directories.addRoot(rootDirectory);

        // Should have been called twice
        verify(registrarFactory, times(2)).newRegistrar(fs);
    }
}
