package ch.sourcepond.io.fileobserver.impl;

import ch.sourcepond.io.fileobserver.api.FileObserver;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Path;
import java.util.Collection;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.*;

/**
 * Created by rolandhauser on 08.02.17.
 */
public class CompoundObserverHandlerTest {
    private static final String ANY_ID = "anyId";
    private final FileObserver observer1 = mock(FileObserver.class);
    private final FileObserver observer2 = mock(FileObserver.class);
    private final ObserverHandler handler1 = mock(ObserverHandler.class);
    private final ObserverHandler handler2 = mock(ObserverHandler.class);
    private final DefaultObserverHandlerFactory factory = mock(DefaultObserverHandlerFactory.class);
    private final FsDirectories fsdirs = mock(FsDirectories.class);
    private final Collection<FsDirectories> directories = asList(fsdirs);
    private final Path file = mock(Path.class);
    private final CompoundObserverHandler compoundHandler = new CompoundObserverHandler(factory);

    @Before
    public void setup() {
        when(factory.newHander(observer1)).thenReturn(handler1);
        when(factory.newHander(observer2)).thenReturn(handler2);
        compoundHandler.putIfAbsent(observer1, directories);
        compoundHandler.putIfAbsent(observer2, directories);
    }

    @Test
    public void putIfAbsent() {
        compoundHandler.putIfAbsent(observer1, directories);

        verify(factory, times(2)).newHander(observer1);
        verify(fsdirs).initialyInformHandler(handler1);
    }

    @Test
    public void modify() {
        compoundHandler.modified(ANY_ID, file);
        verify(handler1).modified(ANY_ID, file);
        verify(handler2).modified(ANY_ID, file);
    }

    @Test
    public void deleted() {
        compoundHandler.deleted(ANY_ID);
        verify(handler1).deleted(ANY_ID);
        verify(handler2).deleted(ANY_ID);
    }

    @Test
    public void remove() {
        compoundHandler.remove(observer1);
        compoundHandler.modified(ANY_ID, file);
        verify(handler1, never()).modified(anyString(), any());
        verify(handler2).modified(ANY_ID, file);
    }
}
