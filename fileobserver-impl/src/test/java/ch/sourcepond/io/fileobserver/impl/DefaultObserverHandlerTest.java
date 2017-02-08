package ch.sourcepond.io.fileobserver.impl;

import ch.sourcepond.io.fileobserver.api.ResourceObserver;
import org.junit.After;
import org.junit.Test;

import java.nio.file.Path;
import java.util.concurrent.ExecutorService;

import static java.lang.Thread.sleep;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.mockito.Mockito.*;

/**
 * Created by rolandhauser on 08.02.17.
 */
public class DefaultObserverHandlerTest {
    private static final String ANY_ID = "anyId";
    private final ResourceObserver delegate = mock(ResourceObserver.class);
    private final ExecutorService observerExecutor = newSingleThreadExecutor();
    private final Path file = mock(Path.class);
    private final DefaultObserverHandlerFactory factory = new DefaultObserverHandlerFactory(observerExecutor);
    private final ObserverHandler handler = factory.newHander(delegate);

    @After
    public void tearDown() {
        observerExecutor.shutdown();
    }

    @Test
    public void modifyDeletedAccepted() {
        when(delegate.accept(ANY_ID, file)).thenReturn(true);
        handler.modified(ANY_ID, file);
        verify(delegate, timeout(1000)).modified(ANY_ID, file);
        handler.deleted(ANY_ID);
        verify(delegate, timeout(1000)).deleted(ANY_ID);
    }

    @Test
    public void doNotModifyOrDeleteWhenNotAccepted() throws Exception {
        handler.modified(ANY_ID, file);
        handler.deleted(ANY_ID);
        sleep(1000);
        verify(delegate, never()).modified(anyString(), any());
        verify(delegate, never()).deleted(anyString());
    }
}
