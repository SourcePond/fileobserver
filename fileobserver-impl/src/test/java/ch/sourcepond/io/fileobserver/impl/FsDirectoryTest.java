package ch.sourcepond.io.fileobserver.impl;

import ch.sourcepond.io.checksum.api.CalculationObserver;
import ch.sourcepond.io.checksum.api.Checksum;
import ch.sourcepond.io.checksum.api.Resource;
import ch.sourcepond.io.checksum.api.ResourcesFactory;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.WatchKey;

import static ch.sourcepond.io.checksum.api.Algorithm.SHA256;
import static ch.sourcepond.io.fileobserver.impl.FsDirectory.TIMEOUT;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.*;

/**
 * Created by rolandhauser on 08.02.17.
 */
public class FsDirectoryTest {
    private static final String ANY_RELATIVIZED_PATH = "anyPath";
    private final ResourcesFactory resourcesFactory = mock(ResourcesFactory.class);
    private final ObserverHandler handler = mock(ObserverHandler.class);
    private final FsDirectoryFactory factory = new FsDirectoryFactory(resourcesFactory);
    private final WatchKey watchKey = mock(WatchKey.class);
    private final WatchKey parentWatchKey = mock(WatchKey.class);
    private final Path parentPath = mock(Path.class);
    private final Path childPath = mock(Path.class);
    private final Path path = mock(Path.class);
    private final Path relativePath = mock(Path.class);
    private final Checksum checksum1 = mock(Checksum.class);
    private final Checksum checksum2 = mock(Checksum.class);
    private final Resource resource = mock(Resource.class);
    private final FsDirectory parent = factory.newDirectory(null, parentWatchKey);
    private final FsDirectory child = factory.newDirectory(parent, watchKey);

    @Before
    public void setup() {
        when(parentWatchKey.watchable()).thenReturn(parentPath);
        when(watchKey.watchable()).thenReturn(childPath);
        when(parentPath.relativize(path)).thenReturn(relativePath);
        when(relativePath.toString()).thenReturn(ANY_RELATIVIZED_PATH);
        when(resourcesFactory.create(SHA256, path)).thenReturn(resource);
    }

    @Test
    public void getPath() {
        assertSame(childPath, child.getPath());
    }

    @Test
    public void relativize() {
        assertEquals(ANY_RELATIVIZED_PATH, child.relativize(path));
    }

    @Test
    public void cancelKey() {
        child.cancelKey();
        verify(watchKey).cancel();;
    }

    @Test
    public void createResourceOnlyOnce() {
        child.informIfChanged(handler, path);
        child.informIfChanged(handler, path);
        verify(resourcesFactory).create(SHA256, path);
    }

    @Test
    public void informAboutChange() {
        when(resource.update(eq(TIMEOUT), notNull())).thenAnswer(im -> {
            final CalculationObserver lambda = im.getArgument(1);
            lambda.done(checksum1, checksum2);
            return null;
        });

        child.informIfChanged(handler, path);
        verify(handler).modified(ANY_RELATIVIZED_PATH, path);
    }

    @Test
    public void doNotinformAboutChangeWhenChecksumsAreEqual() {
        when(resource.update(eq(TIMEOUT), notNull())).thenAnswer(im -> {
            final CalculationObserver lambda = im.getArgument(1);

            // Pass the same checksum
            lambda.done(checksum1, checksum1);
            return null;
        });

        child.informIfChanged(handler, path);
        verify(handler, never()).modified(ANY_RELATIVIZED_PATH, path);
    }
}
