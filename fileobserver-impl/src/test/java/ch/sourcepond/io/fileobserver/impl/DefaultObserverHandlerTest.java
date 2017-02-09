package ch.sourcepond.io.fileobserver.impl;

import ch.sourcepond.commons.smartswitch.testing.SmartSwitchRule;
import ch.sourcepond.io.fileobserver.api.FileObserver;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.nio.file.Path;
import java.util.concurrent.ExecutorService;

import static java.lang.Thread.sleep;
import static org.mockito.Mockito.*;

/**
 * Created by rolandhauser on 08.02.17.
 */
public class DefaultObserverHandlerTest {
    private static final String ANY_ID = "anyId";
    private final FileObserver delegate = mock(FileObserver.class);
    private final Path file = mock(Path.class);

    @Rule
    public final SmartSwitchRule rule = new SmartSwitchRule();

    private ObserverHandler handler;

    @Before
    public void setup() {
        rule.useDefaultService(ExecutorService.class, "(sourcepond.io.fileobserver.observerexecutor=*)");
        handler = new DefaultObserverHandlerFactory(rule.getTestFactory()).newHander(delegate);
    }

    @Test
    public void modifyDeletedAccepted() throws Exception {
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
