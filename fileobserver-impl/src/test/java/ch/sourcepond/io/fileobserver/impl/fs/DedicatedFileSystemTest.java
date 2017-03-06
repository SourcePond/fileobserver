package ch.sourcepond.io.fileobserver.impl.fs;

import ch.sourcepond.io.fileobserver.api.FileObserver;
import ch.sourcepond.io.fileobserver.impl.CopyResourcesTest;
import ch.sourcepond.io.fileobserver.impl.ExecutorServices;
import ch.sourcepond.io.fileobserver.impl.directory.Directory;
import ch.sourcepond.io.fileobserver.impl.directory.DirectoryFactory;
import ch.sourcepond.io.fileobserver.impl.directory.RootDirectory;
import ch.sourcepond.io.fileobserver.spi.WatchedDirectory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.WatchKey;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;

import static ch.sourcepond.io.fileobserver.impl.TestKey.TEST_KEY;
import static java.util.Arrays.asList;
import static java.util.concurrent.Executors.newCachedThreadPool;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by rolandhauser on 06.03.17.
 */
public class DedicatedFileSystemTest extends CopyResourcesTest {
    private final ConcurrentMap<Path, Directory> dirs = new ConcurrentHashMap<>();
    private final ExecutorServices executors = mock(ExecutorServices.class);
    private final ExecutorService executor = newCachedThreadPool();
    private final FileObserver observer = mock(FileObserver.class);
    private final Collection<FileObserver> observers = asList(observer);
    private final DirectoryFactory directoryFactory = mock(DirectoryFactory.class);
    private final WatchedDirectory watchedDirectory = mock(WatchedDirectory.class);
    private final RootDirectory dir = mock(RootDirectory.class);
    private final DirectoryRebase rebase = mock(DirectoryRebase.class);
    private WatchServiceWrapper wsRegistrar = mock(WatchServiceWrapper.class);
    private DedicatedFileSystem fs;

    private static WatchKey matchWatchKey(final Path pPath) {
        return argThat(new ArgumentMatcher<WatchKey>() {
            @Override
            public boolean matches(final WatchKey watchKey) {
                return pPath.equals(watchKey.watchable());
            }

            @Override
            public String toString() {
                return "WatchKey for " + pPath;
            }
        });
    }

    @Before
    public void setup() throws IOException {
        // Setup watched-root_dir_path
        when(watchedDirectory.getKey()).thenReturn(TEST_KEY);
        when(watchedDirectory.getDirectory()).thenReturn(root_dir_path);

        // Setup directories
        when(directoryFactory.newRoot(matchWatchKey(root_dir_path))).thenReturn(dir);
        when(dir.getPath()).thenReturn(root_dir_path);

        // Setup fs
        when(executors.getDirectoryWalkerExecutor()).thenReturn(executor);
        fs = new DedicatedFileSystem(executors, directoryFactory, wsRegistrar, rebase, dirs);
    }

    @After
    public void tearDown() {
        executor.shutdown();
    }

    /**
     *
     */
    @Test(expected = IllegalArgumentException.class)
    public void registeringSameKeyIsNotAllowed() throws IOException {
        fs.registerRootDirectory(watchedDirectory, observers);

        // This should cause an exception
        fs.registerRootDirectory(watchedDirectory, observers);
    }


}
