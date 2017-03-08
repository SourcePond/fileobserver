package ch.sourcepond.io.fileobserver.impl.directory;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Collection;

import static org.junit.Assert.*;

/**
 * Created by rolandhauser on 08.03.17.
 */
public class RootDirectoryTest extends DirectoryTest {
    private Directory root;

    @Before
    public void setup() throws IOException {
        root = factory.newRoot(wrapper.register(subdir_1_path));
    }

    @Test
    public void rebase() throws IOException {
        root.addDirectoryKey(ROOT_DIR_KEY);
        final Directory newRoot = factory.newRoot(wrapper.register(root_dir_path));
        final Directory rc = root.rebase(newRoot);
        assertNotSame(root, rc);
        assertTrue(rc instanceof SubDirectory);
        final SubDirectory rebased = (SubDirectory)rc;
        assertSame(newRoot, rebased.getParent());
        assertSame(root.getWatchKey(), rebased.getWatchKey());
        final Collection<Object> keys = rebased.getDirectoryKeys();
        assertEquals(1, keys.size());
        assertTrue(keys.contains(ROOT_DIR_KEY));
    }

    @Test
    public void hasKeys() {
        // It's assumed that this method returns always true for
        // root directories (event when no keys are added)
        assertTrue(root.hasKeys());
    }

    @Test
    public void toRootDirectory() {
        assertSame(root, root.toRootDirectory());
    }
}
