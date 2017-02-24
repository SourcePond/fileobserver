package ch.sourcepond.io.fileobserver.impl.directory;

import ch.sourcepond.io.checksum.api.CalculationObserver;
import ch.sourcepond.io.checksum.api.Checksum;
import ch.sourcepond.io.checksum.api.Resource;
import ch.sourcepond.io.checksum.api.ResourcesFactory;
import ch.sourcepond.io.fileobserver.api.FileKey;
import ch.sourcepond.io.fileobserver.api.FileObserver;
import ch.sourcepond.io.fileobserver.impl.ExecutorServices;
import ch.sourcepond.io.fileobserver.impl.filekey.DefaultFileKeyFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.WatchKey;
import java.util.Collection;
import java.util.concurrent.ExecutorService;

import static ch.sourcepond.io.checksum.api.Algorithm.SHA256;
import static ch.sourcepond.io.fileobserver.impl.TestKey.TEST_KEY;
import static ch.sourcepond.io.fileobserver.impl.directory.FsDirectory.TIMEOUT;
import static java.util.Arrays.asList;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.*;

/**
 * Created by rolandhauser on 08.02.17.
 */
public class FsDirectoryTest {
    private static final String ANY_RELATIVIZED_PATH = "anyPath";
    private final ExecutorServices executorServices = mock(ExecutorServices.class);
    private final ExecutorService observerExecutor = newSingleThreadExecutor();
    private final DefaultFileKeyFactory fileKeyFactory = mock(DefaultFileKeyFactory.class);
    private final ResourcesFactory resourcesFactory = mock(ResourcesFactory.class);
    private final FileObserver observer = mock(FileObserver.class);
    private final Collection<FileObserver> observers = asList(observer);
    private final FsDirectoryFactory factory = new FsDirectoryFactory(resourcesFactory, fileKeyFactory, executorServices);
    private final WatchKey watchKey = mock(WatchKey.class);
    private final WatchKey parentWatchKey = mock(WatchKey.class);
    private final Path parentPath = mock(Path.class);
    private final Path childPath = mock(Path.class);
    private final Path path = mock(Path.class);
    private final Path relativePath = mock(Path.class);
    private final Checksum checksum1 = mock(Checksum.class);
    private final Checksum checksum2 = mock(Checksum.class);
    private final Resource resource = mock(Resource.class);
    private final FileKey key = mock(FileKey.class);
    private final FsRootDirectory parent = factory.newRoot(TEST_KEY);
    private final FsDirectory child = factory.newBranch(parent, watchKey);

    @Before
    public void setup() {
        when(executorServices.getObserverExecutor()).thenReturn(observerExecutor);
        when(fileKeyFactory.newKey(TEST_KEY, relativePath)).thenReturn(key);
        when(parentWatchKey.watchable()).thenReturn(parentPath);
        when(watchKey.watchable()).thenReturn(childPath);
        when(parentPath.relativize(path)).thenReturn(relativePath);
        when(relativePath.toString()).thenReturn(ANY_RELATIVIZED_PATH);
        when(resourcesFactory.create(SHA256, path)).thenReturn(resource);
        when(child.getPath()).thenReturn(childPath);
        parent.setWatchKey(parentWatchKey);
    }

    @After
    public void tearDown() {
        observerExecutor.shutdown();
    }

    @Test
    public void getWatchedDirectoryKey() {
        assertSame(TEST_KEY, child.getWatchedDirectoryKey());
    }

    @Test
    public void getPath() {
        assertSame(childPath, child.getPath());
    }

    @Test
    public void cancelKey() {
        child.cancelKey();
        verify(watchKey).cancel();;
    }

    @Test
    public void createResourceOnlyOnce() {
        child.informIfChanged(observers, path);
        child.informIfChanged(observers, path);
        verify(resourcesFactory).create(SHA256, path);
    }

    @Test
    public void informAboutChange() {
        when(resource.update(eq(TIMEOUT), notNull())).thenAnswer(im -> {
            final CalculationObserver lambda = im.getArgument(1);
            lambda.done(checksum1, checksum2);
            return null;
        });

        child.informIfChanged(observers, path);
        verify(observer, timeout(500)).modified(key, path);
    }

    @Test
    public void doNotinformAboutChangeWhenChecksumsAreEqual() {
        when(resource.update(eq(TIMEOUT), notNull())).thenAnswer(im -> {
            final CalculationObserver lambda = im.getArgument(1);

            // Pass the same checksum
            lambda.done(checksum1, checksum1);
            return null;
        });

        child.informIfChanged(observers, path);
        verify(observer, never()).modified(key, path);
    }

    @Test
    public void forceInform() {
        child.forceInformObservers(observers, path);
        verify(observer, timeout(500)).modified(key, path);
    }
}
