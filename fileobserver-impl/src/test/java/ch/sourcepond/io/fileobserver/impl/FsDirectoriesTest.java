package ch.sourcepond.io.fileobserver.impl;

import org.junit.Before;
import org.junit.Test;

import java.nio.file.Path;
import java.util.Iterator;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by rolandhauser on 06.02.17.
 */
public class FsDirectoriesTest {
    private final Registrar registrar = mock(Registrar.class);
    private final FsDirectory fsDirectory = mock(FsDirectory.class);
    private final ObserverHandler handler = mock(ObserverHandler.class);
    private final Path directory = mock(Path.class);
    private final FsDirectories fsDirectories = new FsDirectories(registrar);

    @Before
    public void setup() {
        final Iterator<FsDirectory> it = asList(fsDirectory).iterator();
        when(registrar.iterator()).thenReturn(it);
        when(fsDirectory.getPath()).thenReturn(directory);
    }

    @Test
    public void initialyInformHandler() {

    }
}
