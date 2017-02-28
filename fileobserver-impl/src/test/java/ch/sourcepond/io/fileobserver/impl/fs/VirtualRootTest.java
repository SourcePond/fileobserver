package ch.sourcepond.io.fileobserver.impl.fs;

import ch.sourcepond.io.fileobserver.api.FileKey;
import ch.sourcepond.io.fileobserver.api.FileObserver;
import ch.sourcepond.io.fileobserver.impl.ExecutorServices;
import ch.sourcepond.io.fileobserver.impl.directory.Directory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.spi.FileSystemProvider;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static ch.sourcepond.io.fileobserver.impl.TestKey.TEST_KEY;
import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Created by rolandhauser on 06.02.17.
 */
public class VirtualRootTest {

    private class ObserverCollectionMatcher implements ArgumentMatcher<Collection<FileObserver>> {


        @Override
        public boolean matches(final Collection<FileObserver> argument) {
            return argument.size() == 1;
        }
    }

    private final DedicatedFileSystemFactory dedicatedFileSystemFactory = mock(DedicatedFileSystemFactory.class);
    private final DedicatedFileSystem dedicatedFileSystem = mock(DedicatedFileSystem.class);
    private final Directory childDirectory = mock(Directory.class);
    private final WatchService watchService = mock(WatchService.class);
    private final FileSystem fs = mock(FileSystem.class);
    private final Path rootDirectory = mock(Path.class);
    private final BasicFileAttributes rootDirectoryAttrs = mock(BasicFileAttributes.class);
    private final BasicFileAttributes attrs = mock(BasicFileAttributes.class);
    private final Path testPath = mock(Path.class);
    private final FileSystemProvider provider = mock(FileSystemProvider.class);
    private final FileKey fileKey = mock(FileKey.class);
    private final Collection<FileKey> fileKeys = asList(fileKey);
    private final FileObserver observer = mock(FileObserver.class);
    private final ArgumentMatcher<Collection<FileObserver>> observerMatcher = c -> c.size() == 1 && c.contains(observer);
    private final List<DedicatedFileSystem> roots = mock(List.class);
    private final ExecutorServices executorServices = mock(ExecutorServices.class);
    private final ExecutorService observerExecutor = Executors.newSingleThreadExecutor();
    private VirtualRoot virtualRoot = new VirtualRoot(dedicatedFileSystemFactory, executorServices, roots);

    @Before
    public void setup() throws IOException {
        when(executorServices.getObserverExecutor()).thenReturn(observerExecutor);
        when(rootDirectory.getFileSystem()).thenReturn(fs);
        when(testPath.getFileSystem()).thenReturn(fs);
        when(fs.newWatchService()).thenReturn(watchService);
        when(dedicatedFileSystemFactory.newDirectories(fs)).thenReturn(dedicatedFileSystem);
        when(fs.provider()).thenReturn(provider);
        when(provider.readAttributes(rootDirectory, BasicFileAttributes.class)).thenReturn(rootDirectoryAttrs);
        when(rootDirectoryAttrs.isDirectory()).thenReturn(true);
        when(provider.readAttributes(testPath, BasicFileAttributes.class)).thenReturn(attrs);
        when(childDirectory.createKeys(testPath)).thenReturn(fileKeys);
        virtualRoot.addRoot(TEST_KEY, rootDirectory);
    }

    @After
    public void tearDown() {
        observerExecutor.shutdown();
    }

    @Test
    public void addRoot() throws IOException {
        virtualRoot.addObserver(observer);
        verify(dedicatedFileSystem).rootAdded(same(TEST_KEY), same(rootDirectory), argThat(observerMatcher));
    }

    @Test
    public void addRootIOExceptionOccurred() throws IOException {
        virtualRoot = new VirtualRoot(dedicatedFileSystemFactory, executorServices, roots);

        final IOException expected = new IOException();
        doThrow(expected).when(dedicatedFileSystemFactory).newDirectories(fs);
        try {
            virtualRoot.addRoot(TEST_KEY, rootDirectory);
            fail("Exception expected");
        } catch (final IOException e) {
            assertSame(expected, e.getCause().getCause());
        }
    }

    @Test
    public void addRemoveObserver() {
        when(dedicatedFileSystem.getParentDirectory(testPath)).thenReturn(childDirectory);
        virtualRoot.addObserver(observer);
        verify(dedicatedFileSystem).initiallyInformHandler(observer);

        // This should not cause an action
        virtualRoot.addObserver(observer);
        verifyNoMoreInteractions(observer);

        // After remove we should get an action again
        virtualRoot.removeObserver(observer);
        virtualRoot.addObserver(observer);
        verify(dedicatedFileSystem, times(2)).initiallyInformHandler(observer);
    }

    @Test
    public void pathCreatedPathIsADirectory() throws Exception {
        when(attrs.isDirectory()).thenReturn(true);
        virtualRoot.addObserver(observer);
        virtualRoot.pathModified(testPath);
        verify(dedicatedFileSystem).directoryCreated(same(testPath), argThat(observerMatcher));
    }

    @Test
    public void pathDeleted() throws Exception {
        when(dedicatedFileSystem.getParentDirectory(testPath)).thenReturn(childDirectory);
        when(dedicatedFileSystem.directoryDeleted(testPath)).thenReturn(true);

        virtualRoot.addObserver(observer);
        virtualRoot.pathDeleted(testPath);
        verify(observer, timeout(500)).discard(fileKey);

        // Root should still be the same
        virtualRoot.addRoot(TEST_KEY, rootDirectory);

        // Should have been called twice
        verify(dedicatedFileSystemFactory, times(2)).newDirectories(fs);
    }

    @Test
    public void destroy() throws Exception {
        virtualRoot.stop();
        verify(dedicatedFileSystem).close();

        virtualRoot.addRoot(TEST_KEY, rootDirectory);

        // Should have been called twice
        verify(dedicatedFileSystemFactory, times(2)).newDirectories(fs);
    }

    @Test
    public void closeFsDirectoriesIsNull() throws Exception {
        // Should not cause an exception
        virtualRoot.close(null);
        virtualRoot.addRoot(TEST_KEY, rootDirectory);

        // Should have been called exactly once
        verify(dedicatedFileSystemFactory).newDirectories(fs);
    }

    @Test
    public void closeFsDirectories() throws Exception {
        virtualRoot.close(dedicatedFileSystem);
        verify(dedicatedFileSystem).close();
        verify(roots).remove(dedicatedFileSystem);

        virtualRoot.addRoot(TEST_KEY, rootDirectory);

        // Should have been called twice
        verify(dedicatedFileSystemFactory, times(2)).newDirectories(fs);
    }

    @Test
    public void fileModified() {
        when(dedicatedFileSystem.getParentDirectory(testPath)).thenReturn(childDirectory);
        virtualRoot.addObserver(observer);
        virtualRoot.pathModified(testPath);
        verify(childDirectory).informIfChanged(argThat(observerMatcher), same(testPath));
    }
}
