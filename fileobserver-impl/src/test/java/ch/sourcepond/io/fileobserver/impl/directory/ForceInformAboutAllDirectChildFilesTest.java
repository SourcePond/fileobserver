package ch.sourcepond.io.fileobserver.impl.directory;

import ch.sourcepond.io.fileobserver.api.FileKey;
import ch.sourcepond.io.fileobserver.api.FileObserver;
import ch.sourcepond.io.fileobserver.impl.CopyResourcesTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

import static ch.sourcepond.io.fileobserver.impl.TestKey.TEST_KEY;
import static java.nio.file.StandardWatchEventKinds.*;
import static org.mockito.Mockito.*;

/**
 * Created by rolandhauser on 13.02.17.
 */
public class ForceInformAboutAllDirectChildFilesTest extends CopyResourcesTest {
    private final FsDirectoryFactory factory = mock(FsDirectoryFactory.class);
    private final FileKey fileKey = mock(FileKey.class);
    private final FileObserver observer = mock(FileObserver.class);
    private WatchService watchService;
    private WatchKey parentWatchKey;
    private WatchKey key;
    private FsRootDirectory parent;
    private FsDirectory fsDir;

    @Before
    public void setup() throws IOException {
        doAnswer(i -> {
            ((Runnable)i.getArgument(0)).run();
            return null;
        }).when(factory).execute(Mockito.any());
        when(factory.newKey(TEST_KEY, directory.relativize(testfileXml))).thenReturn(fileKey);
        watchService = fs.newWatchService();
        parentWatchKey = directory.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
        key = subDirectory.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
        parent = new FsRootDirectory(factory);
        parent.addDirectoryKey(TEST_KEY);
        parent.setWatchKey(parentWatchKey);
        fsDir = new FsDirectory(parent, key);
    }

    @After
    public void tearDown() throws IOException {
        watchService.close();
    }

    @Test
    public void forceInformAboutAllDirectChildFiles() {
        fsDir.forceInformAboutAllDirectChildFiles(observer);
        verify(observer, timeout(500)).modified(fileKey, testfileXml);
        verifyNoMoreInteractions(observer);
    }
}
