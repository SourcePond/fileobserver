package ch.sourcepond.io.fileobserver.impl.fs;

import ch.sourcepond.io.fileobserver.api.FileObserver;
import ch.sourcepond.io.fileobserver.impl.diff.DiffObserverFactory;
import ch.sourcepond.io.fileobserver.impl.directory.Directory;
import ch.sourcepond.io.fileobserver.impl.directory.DirectoryFactory;
import ch.sourcepond.io.fileobserver.spi.WatchedDirectory;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.spi.FileSystemProvider;
import java.util.Collection;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.*;

/**
 * Created by rolandhauser on 08.03.17.
 */
public class VirtualRootTest {
    private static final Object ROOT_KEY = new Object();
    private static final Object OTHER_KEY = new Object();
    private final WatchedDirectory watchedDir = mock(WatchedDirectory.class);
    private final FileSystem fs = mock(FileSystem.class);
    private final FileSystemProvider provider = mock(FileSystemProvider.class);
    private final BasicFileAttributes attrs = mock(BasicFileAttributes.class);
    private final Path directory = mock(Path.class);
    private final BasicFileAttributes modifiedPathAttrs = mock(BasicFileAttributes.class);
    private final Path modifiedPath = mock(Path.class);
    private final FileObserver observer = mock(FileObserver.class);
    private final DirectoryFactory directoryFactory = mock(DirectoryFactory.class);
    private final DiffObserverFactory diffObserverFactory = mock(DiffObserverFactory.class);
    private final DedicatedFileSystem dedicatedFs = mock(DedicatedFileSystem.class);
    private final DedicatedFileSystemFactory dedicatedFsFactory = mock(DedicatedFileSystemFactory.class);
    private final Directory dir = mock(Directory.class);
    private VirtualRoot virtualRoot = new VirtualRoot(dedicatedFsFactory);

    private Collection<FileObserver> matchObservers() {
        return argThat(col -> col.size() == 1 && col.contains(observer));
    }

    @Before
    public void setup() throws IOException {
        when(modifiedPath.getFileSystem()).thenReturn(fs);
        when(provider.readAttributes(modifiedPath, BasicFileAttributes.class)).thenReturn(modifiedPathAttrs);

        when(directory.getFileSystem()).thenReturn(fs);
        when(fs.provider()).thenReturn(provider);
        when(provider.readAttributes(directory, BasicFileAttributes.class)).thenReturn(attrs);
        when(attrs.isDirectory()).thenReturn(true);

        when(watchedDir.getKey()).thenReturn(ROOT_KEY);
        when(watchedDir.getDirectory()).thenReturn(directory);
        when(dedicatedFsFactory.openFileSystem(virtualRoot, fs)).thenReturn(dedicatedFs);
        when(dedicatedFsFactory.getDirectoryFactory()).thenReturn(directoryFactory);
        when(dedicatedFsFactory.getDiffObserverFactory()).thenReturn(diffObserverFactory);

        virtualRoot.addRoot(watchedDir);
        virtualRoot.addObserver(observer);
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
        verify(dedicatedFs).registerRootDirectory(same(watchedDir), matchObservers());
        verify(watchedDir).addObserver(virtualRoot);
    }

    @Test
    public void addRootDirectoriesCouldNotBeCreated() throws IOException {
        virtualRoot = new VirtualRoot(dedicatedFsFactory);
        final IOException expected = new IOException();
        doThrow(expected).when(dedicatedFsFactory).openFileSystem(virtualRoot, fs);
        try {
            virtualRoot.addRoot(watchedDir);
            fail("Exception expected");
        } catch (final IOException e) {
            final Throwable cause = e.getCause();
            assertSame(expected, cause.getCause());
        }
    }

    @Test
    public void getComposition() {
        final Object[] composition = virtualRoot.getComposition();
        assertEquals(4, composition.length);
        assertSame(virtualRoot, composition[0]);
        assertSame(dedicatedFsFactory, composition[1]);
        assertSame(directoryFactory, composition[2]);
        assertSame(diffObserverFactory, composition[3]);
    }

    @Test(expected = NullPointerException.class)
    public void addObserverIsNull() {
        virtualRoot.addObserver(null);
    }

    @Test
    public void addObserver() {
        verify(dedicatedFs).forceInform(observer);
    }

    @Test
    public void removeObserver() throws IOException {
        reset(dedicatedFs);
        when(watchedDir.getKey()).thenReturn(OTHER_KEY);
        virtualRoot.removeObserver(observer);
        virtualRoot.addRoot(watchedDir);
        verify(dedicatedFs).registerRootDirectory(same(watchedDir), argThat(o -> o.isEmpty()));
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
        virtualRoot = new VirtualRoot(dedicatedFsFactory);

        // This should not cause an exception
        virtualRoot.removeRoot(watchedDir);
    }

    @Test
    public void removeRoot() throws IOException {
        virtualRoot.removeRoot(watchedDir);
        verify(dedicatedFs).unregisterRootDirectory(same(watchedDir), matchObservers());
        verify(watchedDir).removeObserver(virtualRoot);

        // This should not cause an exception
        virtualRoot.addRoot(watchedDir);
    }

    @Test
    public void directoryModified() {
        when(modifiedPathAttrs.isDirectory()).thenReturn(true);
        virtualRoot.pathModified(modifiedPath);
        verify(dedicatedFs).directoryCreated(same(modifiedPath), matchObservers());
    }

    @Test
    public void fileModified() {
        when(dedicatedFs.getDirectory(directory)).thenReturn(dir);
        when(modifiedPath.getParent()).thenReturn(directory);
        virtualRoot.pathModified(modifiedPath);
        verify(dir).informIfChanged(matchObservers(), same(modifiedPath));
    }

    @Test(expected = NullPointerException.class)
    public void fileModifiedDirNotFound() {
        when(modifiedPath.getParent()).thenReturn(directory);
        virtualRoot.pathModified(modifiedPath);
        verify(dir).informIfChanged(matchObservers(), same(modifiedPath));
    }

    @Test(expected = IllegalStateException.class)
    public void fileModifiedNoDedicatedFileSystemForPathFound() {
        final FileSystem otherFs = mock(FileSystem.class);
        when(otherFs.provider()).thenReturn(provider);
        when(modifiedPath.getFileSystem()).thenReturn(otherFs);
        virtualRoot.pathModified(modifiedPath);
    }

    @Test(expected = IllegalStateException.class)
    public void fileDiscardedNoDedicatedFileSystemForPathFound() {
        final FileSystem otherFs = mock(FileSystem.class);
        when(otherFs.provider()).thenReturn(provider);
        when(modifiedPath.getFileSystem()).thenReturn(otherFs);
        virtualRoot.pathDiscarded(modifiedPath);
    }

    @Test
    public void directoryDiscarded() {
        when(modifiedPath.getParent()).thenReturn(directory);
        when(attrs.isDirectory()).thenReturn(true);
        when(dedicatedFs.getDirectory(directory)).thenReturn(dir);
        virtualRoot.pathDiscarded(modifiedPath);
        verify(dir).informDiscard(matchObservers(), same(modifiedPath));
    }

    @Test
    public void directoryDiscardedNoSuchParent() {
        when(modifiedPath.getParent()).thenReturn(directory);
        when(attrs.isDirectory()).thenReturn(true);
        when(dedicatedFs.getDirectory(directory)).thenReturn(null);

        // This should not cause an exception
        virtualRoot.pathDiscarded(modifiedPath);
        verifyZeroInteractions(dir);
    }

    @Test
    public void stop() {
        virtualRoot.stop();
        verify(dedicatedFs).close();
        final FileObserver otherObserver = mock(FileObserver.class);
        virtualRoot.addObserver(otherObserver);
        verifyZeroInteractions(otherObserver);
    }

    @Test
    public void removeFileSystem() {
        virtualRoot.removeFileSystem(dedicatedFs);
        final FileObserver otherObserver = mock(FileObserver.class);
        virtualRoot.addObserver(otherObserver);
        verifyZeroInteractions(otherObserver);
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
        verify(dedicatedFs, never()).destinationChanged(any(), any(), any());
    }

    @Test
    public void destintationChangePreviousAndCurrentDirAreEqual() throws IOException {
        virtualRoot.destinationChanged(watchedDir, directory);
        verify(dedicatedFs, never()).destinationChanged(any(), any(), any());
    }

    @Test
    public void destintationChange() throws IOException {
        final Path newDirectory = mock(Path.class);
        virtualRoot.destinationChanged(watchedDir, newDirectory);
        verify(dedicatedFs).destinationChanged(same(watchedDir), same(newDirectory), matchObservers());
    }

}
