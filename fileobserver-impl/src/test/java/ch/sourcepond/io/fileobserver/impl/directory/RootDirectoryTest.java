package ch.sourcepond.io.fileobserver.impl.directory;

import ch.sourcepond.io.fileobserver.api.FileObserver;
import ch.sourcepond.io.fileobserver.spi.WatchedDirectory;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Collection;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Created by rolandhauser on 08.03.17.
 */
public class RootDirectoryTest extends DirectoryTest {
    private RootDirectory root;

    @Before
    public void setup() throws IOException {
        root = factory.newRoot(wrapper.register(subdir_1_path));
    }

    @Test
    public void ignoreBlacklistedFiles() {
        final FileObserver observer = mock(FileObserver.class);
        when(watchedRootDir.isBlacklisted(subdir_1_path.relativize(testfile_11_xml_path))).thenReturn(true);
        root.addWatchedDirectory(watchedRootDir);
        root.forceInform();
        verifyZeroInteractions(observer);
    }

    @Test
    public void rebase() throws IOException {
        root.addWatchedDirectory(watchedRootDir);
        final Directory newRoot = factory.newRoot(wrapper.register(root_dir_path));
        final Directory rc = root.rebase(newRoot);
        assertNotSame(root, rc);
        assertTrue(rc instanceof SubDirectory);
        final SubDirectory rebased = (SubDirectory)rc;
        assertSame(newRoot, rebased.getParent());
        assertSame(root.getWatchKey(), rebased.getWatchKey());
        final Collection<WatchedDirectory> keys = rebased.getWatchedDirectories();
        assertEquals(1, keys.size());
        assertTrue(keys.contains(watchedRootDir));
    }

    @Test
    public void hasKeys() {
        assertFalse(root.hasKeys());
        root.addWatchedDirectory(watchedRootDir);
        assertTrue(root.hasKeys());
        root.remove(watchedRootDir);
        assertFalse(root.hasKeys());
    }

    @Test
    public void toRootDirectory() {
        assertSame(root, root.toRootDirectory());
    }
}
