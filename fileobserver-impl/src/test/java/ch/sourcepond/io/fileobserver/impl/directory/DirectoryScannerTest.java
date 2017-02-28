package ch.sourcepond.io.fileobserver.impl.directory;

import ch.sourcepond.io.fileobserver.impl.CopyResourcesTest;
import ch.sourcepond.io.fileobserver.impl.fs.DedicatedFileSystem;
import ch.sourcepond.io.fileobserver.impl.fs.VirtualRoot;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.UUID;

import static ch.sourcepond.io.fileobserver.impl.TestKey.TEST_KEY;
import static com.sun.nio.file.SensitivityWatchEventModifier.HIGH;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardWatchEventKinds.*;
import static java.util.Arrays.asList;
import static org.mockito.Mockito.*;

/**
 *
 */
public class DirectoryScannerTest extends CopyResourcesTest {
    private static final String NEW_FILE_NAME = "newfile.txt";
    private final VirtualRoot virtualRoot = mock(VirtualRoot.class);
    private final DedicatedFileSystem child = mock(DedicatedFileSystem.class);
    private final List<DedicatedFileSystem> roots = asList(child);
    private final DirectoryScanner scanner = new DirectoryScanner(virtualRoot);
    private WatchService watchService;
    private WatchKey key;

    @Before
    public void setup() throws Exception {
        when(virtualRoot.getRoots()).thenReturn(roots);
        watchService = fs.newWatchService();
        key = directory.register(watchService, new WatchEvent.Kind[]{
                ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE}, HIGH);
        when(child.poll()).thenReturn(key).thenAnswer(im -> {
            Thread.sleep(1000);
            return watchService.poll();
        });
        virtualRoot.addRoot(TEST_KEY, directory);
        scanner.start();
    }

    @After
    public void tearDown() throws IOException {
        watchService.close();
        virtualRoot.pathDeleted(directory);
        scanner.stop();
    }

    private void changeContent(final Path pPath) throws Exception {
        try (final BufferedWriter writer = Files.newBufferedWriter(pPath, CREATE)) {
            writer.write(UUID.randomUUID().toString());
            Thread.sleep(5000);
        }
        verify(virtualRoot, timeout(15000)).pathModified(pPath);
        reset(virtualRoot);
    }

    @Test
    public void verifyEntryCreate() throws Exception {
        changeContent(directory.resolve(NEW_FILE_NAME));
    }

    @Test
    public void verifyEntryModify() throws Exception {
        changeContent(testfileTxt);
    }

    @Test
    public void verifyEntryDelete() throws Exception {
        final Path file = directory.resolve(NEW_FILE_NAME);
        changeContent(file);
        Files.delete(file);
        Thread.sleep(5000);
        verify(virtualRoot).pathDeleted(file);
    }
}
