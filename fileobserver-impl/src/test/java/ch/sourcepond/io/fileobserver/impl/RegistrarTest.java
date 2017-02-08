package ch.sourcepond.io.fileobserver.impl;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;

import java.io.IOException;
import java.nio.file.*;

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
    private final RegistrarFactory registrarFactory = new RegistrarFactory(directoryFactory);
    private Registrar registrar;

    private PathMatcher hasPath(final Path pPath) {
        return new PathMatcher(pPath);
    }

    @Before
    public void setup() throws IOException {
        when(directoryFactory.newDirectory(isNull(), argThat(hasPath(directory)))).thenReturn(fsDirectory);
        when(directoryFactory.newDirectory(eq(fsDirectory), argThat(hasPath(subDirectory)))).thenReturn(fsSubDirectory);
        registrar = registrarFactory.newRegistrar(fs);
        registrar.directoryCreated(directory, handler);
    }

    @Test
    public void directoryCreated() throws IOException {
        verify(handler).modified("testfile.txt", testfileTxt);
        verify(handler).modified("subdir/testfile.xml", testfileXml);
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
