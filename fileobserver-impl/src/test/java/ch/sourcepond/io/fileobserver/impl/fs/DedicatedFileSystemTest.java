package ch.sourcepond.io.fileobserver.impl.fs;

import ch.sourcepond.io.fileobserver.api.FileObserver;
import ch.sourcepond.io.fileobserver.impl.CopyResourcesTest;
import ch.sourcepond.io.fileobserver.impl.ExecutorServices;
import ch.sourcepond.io.fileobserver.impl.directory.ChildDirectory;
import ch.sourcepond.io.fileobserver.impl.directory.DirectoryFactory;
import ch.sourcepond.io.fileobserver.impl.directory.RootDirectory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static ch.sourcepond.io.fileobserver.impl.TestKey.TEST_KEY;
import static java.lang.Thread.sleep;
import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Created by rolandhauser on 06.02.17.
 */
public class DedicatedFileSystemTest extends CopyResourcesTest {
    private class RootWatchKeyMatcher implements ArgumentMatcher<WatchKey> {

        @Override
        public boolean matches(final WatchKey argument) {
            return directory.equals(argument.watchable());
        }
    }


    private class SubDirWatchKeyMatcher implements ArgumentMatcher<WatchKey> {

        @Override
        public boolean matches(final WatchKey argument) {
            return subDirectory.equals(argument.watchable());
        }
    }


    private final ExecutorServices executorServices = mock(ExecutorServices.class);
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final DirectoryFactory directoryFactory = mock(DirectoryFactory.class);
    private final DedicatedFileSystemFactory dedicatedFileSystemFactory = new DedicatedFileSystemFactory(executorServices, directoryFactory);
    private final FileObserver observer = mock(FileObserver.class);
    private final Collection<FileObserver> observers = asList(observer);
    private final RootDirectory rootFsDir = mock(RootDirectory.class);
    private final ChildDirectory subFsDir = mock(ChildDirectory.class);
    private WatchService watchService;
    private DedicatedFileSystem dedicatedFileSystem;

    @Before
    public void setup() throws Exception {
        when(executorServices.getDirectoryWalkerExecutor()).thenReturn(executorService);
        when(directoryFactory.newRoot()).thenReturn(rootFsDir);
        when(directoryFactory.newBranch(same(rootFsDir), argThat(new DedicatedFileSystemTest.SubDirWatchKeyMatcher()))).thenReturn(subFsDir);
        watchService = fs.newWatchService();
        dedicatedFileSystem = dedicatedFileSystemFactory.newDirectories(fs);
        dedicatedFileSystem.rootAdded(TEST_KEY, directory, observers);
        sleep(500);
    }

    @Test
    public void ignoreSameRoot() {
        dedicatedFileSystem.rootAdded(TEST_KEY, directory, observers);

        // Should have been called exactly once
        verify(directoryFactory).newRoot();
    }

    @Test
    public void rootAddedFirstWins() throws Exception {
        when(directoryFactory.newRoot()).thenAnswer(new Answer<RootDirectory>() {
            @Override
            public RootDirectory answer(final InvocationOnMock invocation) throws Throwable {
                sleep(500);
                return rootFsDir;
            }
        });
        executorService.execute(() -> dedicatedFileSystem.rootAdded(TEST_KEY, directory, observers));
        executorService.execute(() -> dedicatedFileSystem.rootAdded(TEST_KEY, directory, observers));

        sleep(1000);

        // Should have been called only once
        verify(rootFsDir).setWatchKey(argThat(new DedicatedFileSystemTest.RootWatchKeyMatcher()));
    }

    @Test
    public void directoryWalkFailed() throws IOException {
        // Delete directory to provocate an IOException
        deleteResources();

        // This should not cause an exception
        dedicatedFileSystem.directoryCreated(directory, observers);
    }

    @Test
    public void rootAdded() throws Exception {
        verify(rootFsDir).setWatchKey(argThat(new DedicatedFileSystemTest.RootWatchKeyMatcher()));
        verify(rootFsDir, timeout(500)).forceInformObservers(observers, testfileTxt);
        verify(subFsDir, timeout(500)).forceInformObservers(observers, testfileXml);
    }

    @Test
    public void initiallyInformHandler() {
        dedicatedFileSystem.initiallyInformHandler(observer);
        verify(rootFsDir).forceInformAboutAllDirectChildFiles(observer);
        verify(subFsDir).forceInformAboutAllDirectChildFiles(observer);
    }

    @Test
    public void rootDirectoryDeleted() {
        assertTrue(dedicatedFileSystem.directoryDeleted(directory));
        verify(rootFsDir).cancelKey();
        verify(subFsDir).cancelKey();
    }

    @Test
    public void subDirectoryDeleted() {
        assertFalse(dedicatedFileSystem.directoryDeleted(subDirectory));
        verify(rootFsDir, never()).cancelKey();
        verify(subFsDir).cancelKey();
    }

    @Test(expected = NullPointerException.class)
    public void getParentDirectoryParentNotExistent() {
        dedicatedFileSystem.getParentDirectory(directory);
    }

    @Test
    public void getParentDirectory() {
        assertSame(rootFsDir, dedicatedFileSystem.getParentDirectory(subDirectory));
        assertSame(subFsDir, dedicatedFileSystem.getParentDirectory(testfileXml));
    }

    @Test
    public void close() throws IOException {
        dedicatedFileSystem.close();
        assertTrue(dedicatedFileSystem.directoryDeleted(directory));
        verify(rootFsDir, never()).cancelKey();
    }

    @Test
    public void poll() throws Exception {
        Files.delete(testfileTxt);
        sleep(15000);
        assertNotNull(dedicatedFileSystem.poll());
    }
}
