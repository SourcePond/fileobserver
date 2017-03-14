package ch.sourcepond.io.fileobserver.impl.fs;

import ch.sourcepond.io.fileobserver.impl.CopyResourcesTest;
import ch.sourcepond.io.fileobserver.impl.TestKey;
import ch.sourcepond.io.fileobserver.impl.directory.DirectoryFactory;
import ch.sourcepond.io.fileobserver.impl.directory.RootDirectory;
import ch.sourcepond.io.fileobserver.spi.WatchedDirectory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.WatchKey;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.Thread.sleep;
import static java.nio.file.Files.delete;
import static java.nio.file.Files.newBufferedWriter;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.util.Collections.emptyList;
import static java.util.UUID.randomUUID;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Created by rolandhauser on 14.03.17.
 */
public class DedicatedFileSystemFileChangeTest extends CopyResourcesTest {
    private static final String NEW_FILE_NAME = "newfile.txt";
    private final WatchedDirectory watchedDirectory = mock(WatchedDirectory.class);
    private final VirtualRoot virtualRoot = mock(VirtualRoot.class);
    private final RootDirectory directory = mock(RootDirectory.class);
    private final DirectoryFactory directoryFactory = mock(DirectoryFactory.class);
    private final DirectoryRebase rebase = mock(DirectoryRebase.class);
    private final DirectoryRegistrationWalker walker = mock(DirectoryRegistrationWalker.class);
    private DedicatedFileSystem child;
    private volatile Throwable threadKiller;
    private Path file;
    private WatchServiceWrapper wrapper;
    private WatchKey key;

    @Before
    public void setup() throws Exception {
        when(watchedDirectory.getDirectory()).thenReturn(root_dir_path);
        when(watchedDirectory.getKey()).thenReturn(TestKey.TEST_KEY);
        wrapper = new WatchServiceWrapper(fs);
        key = wrapper.register(root_dir_path);
        when(directoryFactory.newRoot(key)).thenReturn(directory);
        child = new DedicatedFileSystem(virtualRoot, directoryFactory, wrapper, rebase, walker, new ConcurrentHashMap<>());
        child.registerRootDirectory(watchedDirectory, emptyList());

        file = root_dir_path.resolve(NEW_FILE_NAME);
        child.start();
    }

    @After
    public void tearDown() throws IOException {
        child.close();
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
        delete(file);
        verify(virtualRoot, timeout(15000)).pathDiscarded(file);
    }
}
