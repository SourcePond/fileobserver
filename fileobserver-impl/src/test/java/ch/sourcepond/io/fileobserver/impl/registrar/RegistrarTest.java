package ch.sourcepond.io.fileobserver.impl.registrar;

import ch.sourcepond.io.fileobserver.impl.CopyResourcesTest;
import ch.sourcepond.io.fileobserver.impl.directory.FsDirectory;
import ch.sourcepond.io.fileobserver.impl.directory.FsDirectoryFactory;
import ch.sourcepond.io.fileobserver.impl.directory.FsRootDirectory;
import ch.sourcepond.io.fileobserver.impl.observer.ObserverHandler;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static ch.sourcepond.io.fileobserver.impl.TestKey.TEST_KEY;
import static java.lang.Thread.sleep;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

/**
 * Created by rolandhauser on 07.02.17.
 */
public class RegistrarTest extends CopyResourcesTest {

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


    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final FsDirectoryFactory directoryFactory = mock(FsDirectoryFactory.class);
    private RegistrarFactory registrarFactory = new RegistrarFactory(executorService, directoryFactory);
    private final ObserverHandler handler = mock(ObserverHandler.class);
    private final FsRootDirectory rootFsDir = mock(FsRootDirectory.class);
    private final FsDirectory subFsDir = mock(FsDirectory.class);
    private WatchService watchService;
    private Registrar registrar;

    @Before
    public void setup() throws Exception {
        when(directoryFactory.newRoot(TEST_KEY)).thenReturn(rootFsDir);
        when(directoryFactory.newBranch(same(rootFsDir), argThat(new SubDirWatchKeyMatcher()))).thenReturn(subFsDir);
        watchService = fs.newWatchService();
        registrar = registrarFactory.newRegistrar(fs);
        registrar.rootAdded(TEST_KEY, directory, handler);
        sleep(500);
    }

    @Test
    public void ignoreSameRoot() {
        registrar.rootAdded(TEST_KEY, directory, handler);

        // Should have been called exactly once
        verify(directoryFactory).newRoot(TEST_KEY);
    }

    @Test
    public void rootAddedFirstWins() throws Exception {
        when(directoryFactory.newRoot(TEST_KEY)).thenAnswer(new Answer<FsRootDirectory>() {
            @Override
            public FsRootDirectory answer(final InvocationOnMock invocation) throws Throwable {
                sleep(500);
                return rootFsDir;
            }
        });
        executorService.execute(() -> registrar.rootAdded(TEST_KEY, directory, handler));
        executorService.execute(() -> registrar.rootAdded(TEST_KEY, directory, handler));

        sleep(1000);

        // Should have been called only once
        verify(rootFsDir).setWatchKey(argThat(new RootWatchKeyMatcher()));
    }

    @Test
    public void directoryWalkFailed() throws IOException {
        // Delete directory to provocate an IOException
        deleteResources();

        // This should not cause an exception
        registrar.directoryCreated(directory, handler);
    }

    @Test
    public void rootAdded() throws Exception {
        verify(rootFsDir).setWatchKey(argThat(new RootWatchKeyMatcher()));
        verify(rootFsDir, timeout(500)).forceInform(handler, testfileTxt);
        verify(subFsDir, timeout(500)).forceInform(handler, testfileXml);
    }

    @Test
    public void initiallyInformHandler() {
        registrar.initiallyInformHandler(handler);
        verify(rootFsDir).forceInform(handler);
        verify(subFsDir).forceInform(handler);
    }

    @Test
    public void rootDirectoryDeleted() {
        assertTrue(registrar.directoryDeleted(directory));
        verify(rootFsDir).cancelKey();
        verify(subFsDir).cancelKey();
    }

    @Test
    public void subDirectoryDeleted() {
        assertFalse(registrar.directoryDeleted(subDirectory));
        verify(rootFsDir, never()).cancelKey();
        verify(subFsDir).cancelKey();
    }

    @Test
    public void getDirectory() {
        assertNotNull(registrar.getDirectory(directory));
        assertNotNull(registrar.getDirectory(subDirectory));
    }

    @Test
    public void close() throws IOException {
        registrar.close();
        assertTrue(registrar.directoryDeleted(directory));
        verify(rootFsDir, never()).cancelKey();
    }

    @Test
    public void poll() throws Exception {
        Files.delete(testfileTxt);
        sleep(5000);
        assertNotNull(registrar.poll());
    }
}
