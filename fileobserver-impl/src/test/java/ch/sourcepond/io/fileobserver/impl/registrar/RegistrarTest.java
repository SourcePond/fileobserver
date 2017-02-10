package ch.sourcepond.io.fileobserver.impl.registrar;

import ch.sourcepond.io.fileobserver.api.FileKey;
import ch.sourcepond.io.fileobserver.impl.CopyResourcesTest;
import ch.sourcepond.io.fileobserver.impl.observer.ObserverHandler;
import ch.sourcepond.io.fileobserver.impl.directory.FsDirectory;
import ch.sourcepond.io.fileobserver.impl.directory.FsDirectoryFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;

import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchKey;

import static ch.sourcepond.io.fileobserver.impl.TestKey.TEST_KEY;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

/**
 * Created by rolandhauser on 07.02.17.
 */
public class RegistrarTest extends CopyResourcesTest {

    private static class PathMatcher implements ArgumentMatcher<WatchKey> {
        private final Path path;

        PathMatcher(final Path pPath) {
            path = pPath;
        }

        @Override
        public boolean matches(final WatchKey key) {
            return null != key && path.equals(key.watchable());
        }
    }

    private final FsDirectoryFactory directoryFactory = mock(FsDirectoryFactory.class);
    private final ObserverHandler handler = mock(ObserverHandler.class);
    private final FsDirectory fsDirectory = mock(FsDirectory.class);
    private final FsDirectory fsSubDirectory = mock(FsDirectory.class);

    private final FileKey key = mock(FileKey.class);
    private final FileKey subKey = mock(FileKey.class);

    private final RegistrarFactory registrarFactory = new RegistrarFactory(directoryFactory);
    private Registrar registrar;

    private PathMatcher hasPath(final Path pPath) {
        return new PathMatcher(pPath);
    }

    @Before
    public void setup() throws IOException {
        when(directoryFactory.newDirectory(TEST_KEY, isNull(), argThat(hasPath(directory)))).thenReturn(fsDirectory);
        when(directoryFactory.newDirectory(TEST_KEY, eq(fsDirectory), argThat(hasPath(subDirectory)))).thenReturn(fsSubDirectory);
        registrar = registrarFactory.newRegistrar(fs);
        registrar.directoryCreated(TEST_KEY, directory, handler);
    }

    @Test
    public void directoryCreated() throws IOException {
        verify(handler).modified(key, testfileTxt);
        verify(handler).modified(subKey, testfileXml);
        assertSame(fsDirectory, registrar.get(directory));
        assertSame(fsSubDirectory, registrar.get(subDirectory));
        verifyNoMoreInteractions(handler);
    }

    @Test
    public void subDirDirectoryDeleted() {
        assertFalse(registrar.directoryDeleted(subDirectory));
        verify(fsSubDirectory).cancelKey();
        assertNull(registrar.get(subDirectory));
        verifyZeroInteractions(fsDirectory);
    }

    @Test
    public void directoryDeleted() {
        assertTrue(registrar.directoryDeleted(directory));
        verify(fsDirectory).cancelKey();
        assertNull(registrar.get(directory));
        assertNull(registrar.get(subDirectory));
    }

    @Test
    public void close() throws IOException {
        registrar.close();
        try {
            registrar.poll();
            fail("Exception expected");
        } catch (final ClosedWatchServiceException e) {
            // expected
        }
        assertNull(registrar.get(directory));
        assertNull(registrar.get(subDirectory));
    }

    @Test
    public void poll() throws Exception{
        Files.delete(testfileTxt);
        Thread.sleep(5000);
        WatchKey key = registrar.poll();
        assertNotNull(key);
    }

}
