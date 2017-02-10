package ch.sourcepond.io.fileobserver.impl.observer;

import ch.sourcepond.io.fileobserver.api.FileKey;
import ch.sourcepond.io.fileobserver.api.FileObserver;
import ch.sourcepond.io.fileobserver.impl.directory.FsDirectories;
import ch.sourcepond.io.fileobserver.impl.observer.CompoundObserverHandler;
import ch.sourcepond.io.fileobserver.impl.observer.DefaultObserverHandlerFactory;
import ch.sourcepond.io.fileobserver.impl.observer.ObserverHandler;
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
    private final FileObserver observer1 = mock(FileObserver.class);
    private final FileObserver observer2 = mock(FileObserver.class);
    private final ObserverHandler handler1 = mock(ObserverHandler.class);
    private final ObserverHandler handler2 = mock(ObserverHandler.class);
    private final DefaultObserverHandlerFactory factory = mock(DefaultObserverHandlerFactory.class);
    private final FsDirectories fsdirs = mock(FsDirectories.class);
    private final Collection<FsDirectories> directories = asList(fsdirs);
    private final Path file = mock(Path.class);
    private final FileKey key = mock(FileKey.class);
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
        verify(fsdirs).initiallyInformHandler(handler1);
    }

    @Test
    public void modify() {
        compoundHandler.modified(key, file);
        verify(handler1).modified(key, file);
        verify(handler2).modified(key, file);
    }

    @Test
    public void deleted() {
        compoundHandler.deleted(key);
        verify(handler1).deleted(key);
        verify(handler2).deleted(key);
    }

    @Test
    public void remove() {
        compoundHandler.remove(observer1);
        compoundHandler.modified(key, file);
        verify(handler1, never()).modified(any(), any());
        verify(handler2).modified(key, file);
    }
}
