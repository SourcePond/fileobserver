package ch.sourcepond.io.fileobserver.impl.directory;

import ch.sourcepond.io.fileobserver.api.FileKey;
import ch.sourcepond.io.fileobserver.api.FileObserver;
import ch.sourcepond.io.fileobserver.impl.observer.CompoundObserverHandler;
import ch.sourcepond.io.fileobserver.impl.registrar.Registrar;
import ch.sourcepond.io.fileobserver.impl.registrar.RegistrarFactory;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.spi.FileSystemProvider;
import java.util.List;

import static ch.sourcepond.io.fileobserver.impl.TestKey.TEST_KEY;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Created by rolandhauser on 06.02.17.
 */
public class DirectoriesTest {
    private final Path relativePath = mock(Path.class);
    private final FsDirectoriesFactory fsDirectoriesFactory = mock(FsDirectoriesFactory.class);
    private final FsDirectories fsDirectories = mock(FsDirectories.class);
    private final FsDirectory fsDirectory = mock(FsDirectory.class);
    private final FileObserver observer = mock(FileObserver.class);
    private final WatchService watchService = mock(WatchService.class);
    private final FileSystem fs = mock(FileSystem.class);
    private final Path rootDirectory = mock(Path.class);
    private final BasicFileAttributes rootDirectoryAttrs = mock(BasicFileAttributes.class);
    private final BasicFileAttributes attrs = mock(BasicFileAttributes.class);
    private final Path testPath = mock(Path.class);
    private final FileSystemProvider provider = mock(FileSystemProvider.class);
    private final WatchKey watchKey = mock(WatchKey.class);
    private final FileKey fileKey = mock(FileKey.class);
    private final RegistrarFactory registrarFactory = mock(RegistrarFactory.class);
    private final Registrar registrar = mock(Registrar.class);
    private final CompoundObserverHandler compoundObserverHandler = mock(CompoundObserverHandler.class);
    private final List<FsDirectories> roots = mock(List.class);
    private Directories directories = new Directories(registrarFactory, compoundObserverHandler, fsDirectoriesFactory, roots);

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
        directories.addRoot(TEST_KEY, rootDirectory);
    }

    @Test
    public void addRoot() throws IOException {
        directories.addRoot(TEST_KEY, rootDirectory);
        verify(fsDirectories).rootAdded(TEST_KEY, rootDirectory, compoundObserverHandler);
    }

    @Test
    public void addRootIOExceptionOccurred() throws IOException {
        directories = new Directories(registrarFactory, compoundObserverHandler, fsDirectoriesFactory, roots);

        final IOException expected = new IOException();
        doThrow(expected).when(registrarFactory).newRegistrar(fs);
        try {
            directories.addRoot(TEST_KEY, rootDirectory);
            fail("Exception expected");
        } catch (final IOException e) {
            assertSame(expected, e.getCause().getCause());
        }
    }


    @Test
    public void addRootPathIsNotADirectory() throws IOException {
        directories = new Directories(registrarFactory, compoundObserverHandler, fsDirectoriesFactory, roots);
        when(rootDirectoryAttrs.isDirectory()).thenReturn(false);

        try {
            directories.addRoot(TEST_KEY,rootDirectory);
            fail("Exception expected");
        } catch (final IllegalArgumentException e) {
            // noop
        }
    }

    @Test
    public void removeRoot() throws IOException {
        when(fsDirectories.directoryDeleted(rootDirectory)).thenReturn(true);
        directories.removeRoot(rootDirectory);
        directories.addRoot(TEST_KEY,rootDirectory);
        verify(registrarFactory, times(2)).newRegistrar(fs);
    }

    @Test
    public void addObserver() {
        when(fsDirectories.getDirectory(testPath)).thenReturn(fsDirectory);
        directories.addObserver(observer);
//        verify(compoundObserverHandler).putIfAbsent(observer, );
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
        directories.pathModified(testPath);
        verify(fsDirectories).directoryCreated(testPath, compoundObserverHandler);
    }

    @Test
    public void pathDeleted() throws Exception {
        when(fsDirectories.getDirectory(testPath)).thenReturn(fsDirectory);
        when(fsDirectories.directoryDeleted(testPath)).thenReturn(true);

        directories.addObserver(observer);
        directories.pathDeleted(testPath);
        verify(compoundObserverHandler).deleted(fileKey);

        // Root should still be the same
        directories.addRoot(TEST_KEY, rootDirectory);

        // Should have been called twice
        verify(registrarFactory, times(2)).newRegistrar(fs);
    }

    @Test
    public void close() throws Exception {
        directories.close();
        verify(fsDirectories).close();

        directories.addRoot(TEST_KEY, rootDirectory);

        // Should have been called twice
        verify(registrarFactory, times(2)).newRegistrar(fs);
    }

    @Test
    public void closeFsDirectoriesIsNull() throws Exception {
        // Should not cause an exception
        directories.close(null);
        directories.addRoot(TEST_KEY, rootDirectory);

        // Should have been called exactly once
        verify(registrarFactory).newRegistrar(fs);
    }

    @Test
    public void closeFsDirectories() throws Exception {
        directories.close(fsDirectories);
        verify(fsDirectories).close();
        verify(roots).remove(fsDirectories);

        directories.addRoot(TEST_KEY, rootDirectory);

        // Should have been called twice
        verify(registrarFactory, times(2)).newRegistrar(fs);
    }
}
