package ch.sourcepond.io.fileobserver.impl;

import java.nio.file.WatchService;

import static org.mockito.Mockito.mock;

/**
 * Created by rolandhauser on 06.02.17.
 */
public class FsDirectoriesTest {
    private final Registrar registrar = mock(Registrar.class);
    private final DefaultObserverHandler handler = mock(DefaultObserverHandler.class);
    private final FsDirectories fsDirectories = new FsDirectories(registrar);
}
