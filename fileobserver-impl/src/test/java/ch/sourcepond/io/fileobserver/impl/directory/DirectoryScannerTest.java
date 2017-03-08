package ch.sourcepond.io.fileobserver.impl.directory;

import ch.sourcepond.io.fileobserver.impl.CopyResourcesTest;
import ch.sourcepond.io.fileobserver.impl.fs.DedicatedFileSystem;
import ch.sourcepond.io.fileobserver.impl.fs.VirtualRoot;
import ch.sourcepond.io.fileobserver.spi.WatchedDirectory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static ch.sourcepond.io.fileobserver.impl.TestKey.TEST_KEY;
import static com.sun.nio.file.SensitivityWatchEventModifier.HIGH;
import static java.lang.Thread.setDefaultUncaughtExceptionHandler;
import static java.lang.Thread.sleep;
import static java.nio.file.Files.newBufferedWriter;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardWatchEventKinds.*;
import static java.time.Clock.systemUTC;
import static java.util.UUID.randomUUID;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 */
public class DirectoryScannerTest extends CopyResourcesTest {
    private static final String NEW_FILE_NAME = "newfile.txt";
    private final WatchedDirectory watchedDirectory = mock(WatchedDirectory.class);
    private final VirtualRoot virtualRoot = mock(VirtualRoot.class);
    private final DedicatedFileSystem child = mock(DedicatedFileSystem.class);
    private final List<DedicatedFileSystem> roots = new CopyOnWriteArrayList<>();
    private final DirectoryScanner scanner = new DirectoryScanner(systemUTC(), virtualRoot);
    private volatile Throwable threadKiller;
    private Path file;
    private WatchService watchService;
    private WatchKey key;

    @Before
    public void setup() throws Exception {
        roots.add(child);
        when(watchedDirectory.getDirectory()).thenReturn(root_dir_path);
        when(watchedDirectory.getKey()).thenReturn(TEST_KEY);
        when(virtualRoot.getRoots()).thenReturn(roots);
        watchService = fs.newWatchService();
        key = root_dir_path.register(watchService, new WatchEvent.Kind[]{
                ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE}, HIGH);
        file = root_dir_path.resolve(NEW_FILE_NAME);
        when(child.poll()).thenReturn(key).thenAnswer(im -> {
            sleep(1000);
            return watchService.poll();
        });

        setDefaultUncaughtExceptionHandler((t, e) -> threadKiller = e);

        virtualRoot.addRoot(watchedDirectory);
        scanner.start();
    }

    @After
    public void tearDown() throws IOException {
        watchService.close();
        virtualRoot.pathDiscarded(root_dir_path);
        scanner.stop();
    }

    private void writeContent(final Path pPath) throws Exception {
        try (final BufferedWriter writer = newBufferedWriter(pPath, CREATE)) {
            writer.write(randomUUID().toString());
            sleep(5000);
        }
    }

    private void changeContent(final Path pPath) throws Exception {
        writeContent(pPath);
        verify(virtualRoot, timeout(15000)).pathModified(pPath);
        reset(virtualRoot);
    }

    @Test
    public void verifyThreadNotKillWhenRuntimeExceptionOccurs() throws Exception {
        doThrow(RuntimeException.class).when(virtualRoot).pathModified(any());
        writeContent(file);
        verify(virtualRoot, timeout(15000)).pathModified(file);
        assertNull(threadKiller);
    }

    @Test
    public void closeAndRemoveFsWhenWatchServiceClosed() {
        doAnswer(inv -> {
            roots.remove(inv.getArgument(0));
            return null;
        }).when(virtualRoot).close(child);
        final ClosedWatchServiceException expected = new ClosedWatchServiceException();
        doThrow(expected).when(child).poll();
        verify(virtualRoot, timeout(10000)).close(child);
    }

    @Test
    public void pathDiscardedBecauseWatchKeyClosed() throws Exception {
        doAnswer(inv -> {
            key.cancel();
            return null;
        }).when(virtualRoot).pathModified(file);
        writeContent(file);
        verify(virtualRoot, timeout(15000)).pathDiscarded(root_dir_path);
    }

    @Test
    public void entryCreate() throws Exception {
        changeContent(file);
    }

    @Test
    public void entryModify() throws Exception {
        changeContent(testfile_txt_path);
    }

    @Test
    public void entryDelete() throws Exception {
        changeContent(file);
        Files.delete(file);
        sleep(5000);
        verify(virtualRoot).pathDiscarded(file);
    }
}
