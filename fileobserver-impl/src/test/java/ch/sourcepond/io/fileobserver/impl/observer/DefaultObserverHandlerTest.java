package ch.sourcepond.io.fileobserver.impl.observer;

import ch.sourcepond.commons.smartswitch.testing.SmartSwitchRule;
import ch.sourcepond.io.fileobserver.api.FileKey;
import ch.sourcepond.io.fileobserver.api.FileObserver;
import ch.sourcepond.io.fileobserver.impl.observer.DefaultObserverHandlerFactory;
import ch.sourcepond.io.fileobserver.impl.observer.ObserverHandler;
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
    private final FileKey key = mock(FileKey.class);
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
        when(delegate.accept(key, file)).thenReturn(true);
        handler.modified(key, file);
        verify(delegate, timeout(1000)).modified(key, file);
        handler.deleted(key);
        verify(delegate, timeout(1000)).deleted(key);
    }

    @Test
    public void doNotModifyOrDeleteWhenNotAccepted() throws Exception {
        handler.modified(key, file);
        handler.deleted(key);
        sleep(1000);
        verify(delegate, never()).modified(any(), any());
        verify(delegate, never()).deleted(any());
    }
}
