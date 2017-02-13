package ch.sourcepond.io.fileobserver.impl.directory;

import ch.sourcepond.io.fileobserver.api.FileKey;
import ch.sourcepond.io.fileobserver.api.FileObserver;
import ch.sourcepond.io.fileobserver.impl.CopyResourcesTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Collection;

import static java.nio.file.StandardWatchEventKinds.*;
import static java.util.Arrays.asList;
import static org.mockito.Mockito.*;

/**
 * Created by rolandhauser on 13.02.17.
 */
public class ForceInformAboutAllDirectChildFilesTest extends CopyResourcesTest {
    private final FileKey fileKey = mock(FileKey.class);
    private final FsBaseDirectory parent = mock(FsBaseDirectory.class);
    private final FileObserver observer = mock(FileObserver.class);
    private final Collection<FileObserver> observers = asList(observer);
    private WatchService watchService;
    private WatchKey key;
    private FsDirectory fsDir;

    @Before
    public void setup() throws IOException {
        when(parent.newKey(testfileTxt)).thenReturn(fileKey);
        watchService = fs.newWatchService();
        key = directory.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
        fsDir = new FsDirectory(parent, key);
    }

    @After
    public void tearDown() throws IOException {
        watchService.close();
    }

    @Test
    public void forceInformAboutAllDirectChildFiles() {
        fsDir.forceInformAboutAllDirectChildFiles(observers);
        verify(observer).modified(fileKey, testfileTxt);
        verifyNoMoreInteractions(observer);
    }

}
