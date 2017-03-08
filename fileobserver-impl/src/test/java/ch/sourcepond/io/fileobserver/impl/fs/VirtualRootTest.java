package ch.sourcepond.io.fileobserver.impl.fs;

import ch.sourcepond.io.fileobserver.impl.directory.DirectoryFactory;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by rolandhauser on 08.03.17.
 */
public class VirtualRootTest {
    private final DirectoryFactory directoryFactory = mock(DirectoryFactory.class);
    private final DedicatedFileSystemFactory fsFactory = mock(DedicatedFileSystemFactory.class);
    private final VirtualRoot virtualRoot = new VirtualRoot(fsFactory);

    @Before
    public void setup() {
        when(fsFactory.getDirectoryFactory()).thenReturn(directoryFactory);
    }

    @Test
    public void getComposition() {
        final Object[] composition = virtualRoot.getComposition();

    }
}
