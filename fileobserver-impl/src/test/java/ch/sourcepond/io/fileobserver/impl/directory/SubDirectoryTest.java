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
import static ch.sourcepond.io.fileobserver.impl.TestKey.TEST_KEY1;
import static ch.sourcepond.io.fileobserver.impl.directory.SubDirectory.TIMEOUT;
import static java.util.Arrays.asList;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.*;

/**
 * Created by rolandhauser on 08.02.17.
 */
public class SubDirectoryTest {
    private static final String ANY_RELATIVIZED_PATH = "anyPath";
    private final ExecutorServices executorServices = mock(ExecutorServices.class);
    private final ExecutorService observerExecutor = newSingleThreadExecutor();
    private final DefaultFileKeyFactory fileKeyFactory = mock(DefaultFileKeyFactory.class);
    private final ResourcesFactory resourcesFactory = mock(ResourcesFactory.class);
    private final FileObserver observer = mock(FileObserver.class);
    private final Collection<FileObserver> observers = asList(observer);
    private final DirectoryFactory factory = new DirectoryFactory(resourcesFactory, fileKeyFactory, executorServices);
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
    private final RootDirectory parent = factory.newRoot();
    private final Directory child = factory.newBranch(parent, watchKey);

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
    public void getDirectoryKeysNoKeysToChildAdded() {
        parent.addDirectoryKey(TEST_KEY);
        final Collection<Object> keys = child.getDirectoryKeys();
        assertEquals(1, keys.size());
        assertTrue(keys.contains(TEST_KEY));
    }

    @Test
    public void getDirectoryKeysOnlyToChildAdded() {
        child.addDirectoryKey(TEST_KEY);
        final Collection<Object> keys = child.getDirectoryKeys();
        assertEquals(1, keys.size());
        assertTrue(keys.contains(TEST_KEY));
    }

    @Test
    public void getDirectoryKeysChildAndParentHaveKeys() {
        parent.addDirectoryKey(TEST_KEY);
        child.addDirectoryKey(TEST_KEY1);
        final Collection<Object> keys = child.getDirectoryKeys();
        assertEquals(2, keys.size());
        assertTrue(keys.contains(TEST_KEY));
        assertTrue(keys.contains(TEST_KEY1));
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
        parent.addDirectoryKey(TEST_KEY);
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
        parent.addDirectoryKey(TEST_KEY);
       // child.forceInformObservers(observers, path);
        verify(observer, timeout(500)).modified(key, path);
    }
}
